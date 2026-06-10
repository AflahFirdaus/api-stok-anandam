package com.stok.anandam.store.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.model.UserBiometricCredential;
import com.stok.anandam.store.core.postgres.repository.UserBiometricCredentialRepository;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.BiometricDto.AuthResponse;
import com.stok.anandam.store.dto.BiometricDto.ChallengeRequest;
import com.stok.anandam.store.dto.BiometricDto.ChallengeResponse;
import com.stok.anandam.store.dto.BiometricDto.RegisterRequest;
import com.stok.anandam.store.dto.BiometricDto.VerifyRequest;
import com.stok.anandam.store.exception.GlobalExceptionHandler.BiometricAuthenticationException;
import com.stok.anandam.store.util.JwtUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BiometricAuthenticationService {

    private final UserBiometricCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    // Gantilah map ini dengan Redis Cache di tingkat produksi (TTL 60 detik)
    private final Map<String, String> challengeCache = new ConcurrentHashMap<>();

    public BiometricAuthenticationService(UserBiometricCredentialRepository credentialRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void registerBiometric(Long userId, RegisterRequest request) {
        // Hapus key lama jika ada (aktif maupun tidak aktif) untuk menghindari pelanggaran constraint unik device_id
        credentialRepository.findByDeviceId(request.deviceId())
                .ifPresent(cred -> {
                    credentialRepository.delete(cred);
                    credentialRepository.flush(); // Lakukan flush segera agar row terhapus sebelum save data baru
                });

        UserBiometricCredential credential = new UserBiometricCredential();
        credential.setUserId(userId);
        credential.setDeviceId(request.deviceId());
        credential.setDeviceName(request.deviceName());
        credential.setPublicKey(request.publicKey());
        credential.setActive(true);

        credentialRepository.save(credential);
    }

    public ChallengeResponse generateChallenge(ChallengeRequest request) {
        // Pastikan perangkat terdaftar sebelum membuat challenge
        credentialRepository.findByDeviceIdAndActiveTrue(request.deviceId())
                .orElseThrow(() -> new BiometricAuthenticationException("Device not registered for biometric login"));

        SecureRandom random = new SecureRandom();
        byte[] values = new byte[32];
        random.nextBytes(values);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(values);

        // Simpan ke cache dengan key = challenge, value = deviceId
        challengeCache.put(challenge, request.deviceId());
        
        return new ChallengeResponse(challenge);
    }

    public AuthResponse verifyAndAuthenticate(VerifyRequest request) {
        // 1. Validasi Challenge dari Cache
        String cachedDeviceId = challengeCache.get(request.challenge());
        if (cachedDeviceId == null || !cachedDeviceId.equals(request.deviceId())) {
            throw new BiometricAuthenticationException("Invalid or expired challenge");
        }

        // 2. Ambil Kunci Publik dari Database
        UserBiometricCredential credential = credentialRepository.findByDeviceIdAndActiveTrue(request.deviceId())
                .orElseThrow(() -> new BiometricAuthenticationException("Biometric credential not found"));

        // 3. Verifikasi Tanda Tangan Digital (Asymmetric Verification)
        boolean isSignatureValid = verifySignature(credential.getPublicKey(), request.challenge(), request.signature());
        if (!isSignatureValid) {
            throw new BiometricAuthenticationException("Cryptographic signature verification failed");
        }

        // 4. Hapus Challenge dari Cache (Single-use Token Pattern)
        challengeCache.remove(request.challenge());

        // 5. Generate JWT Token Baru untuk User
        String jwtToken = generateJwtTokenForUser(credential.getUserId()); 

        return new AuthResponse(jwtToken, "Bearer");
    }

    private boolean verifySignature(String publicKeyStr, String plainData, String signatureStr) {
        try {
            byte[] publicKeysBytes = Base64.getDecoder().decode(publicKeyStr.trim());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeysBytes);

            // Deteksi algoritma kunci publik dari SubjectPublicKeyInfo OID
            String algorithm = detectAlgorithm(publicKeysBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Tentukan algoritma signature yang sesuai
            String signatureAlgorithm = algorithm.equals("EC") ? "SHA256withECDSA" : "SHA256withRSA";
            Signature sig = Signature.getInstance(signatureAlgorithm);
            sig.initVerify(publicKey);
            sig.update(plainData.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signatureStr.trim());
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            throw new BiometricAuthenticationException("Error verifying cryptographic identity: " + e.getMessage());
        }
    }

    /**
     * Mendeteksi algoritma kunci publik (RSA atau EC) dari byte encoded X.509.
     * Melihat OID (Object Identifier) pada SubjectPublicKeyInfo.
     * 
     * Format X.509 SubjectPublicKeyInfo:
     *   SEQUENCE {
     *       SEQUENCE {        // AlgorithmIdentifier
     *           OID           // 1.2.840.113549.1.1.1 (RSA) atau 1.2.840.10045.2.1 (EC)
     *           ...
     *       },
     *       BIT STRING        // Public key
     *   }
     * 
     * OID RSA: 2A 86 48 86 F7 0D 01 01 01
     * OID EC:  2A 86 48 CE 3D 02 01
     */
    private String detectAlgorithm(byte[] encodedKey) {
        // Cari OID RSA (2A 86 48 86 F7 0D 01 01 01) dalam encoded key
        byte[] rsaOid = new byte[]{0x2A, (byte)0x86, 0x48, (byte)0x86, (byte)0xF7, 0x0D, 0x01, 0x01, 0x01};
        byte[] ecOid  = new byte[]{0x2A, (byte)0x86, 0x48, (byte)0xCE, 0x3D, 0x02, 0x01};

        if (containsBytes(encodedKey, rsaOid)) {
            return "RSA";
        }
        if (containsBytes(encodedKey, ecOid)) {
            return "EC";
        }

        // Fallback ke RSA jika tidak terdeteksi
        return "RSA";
    }

    /** Helper: memeriksa apakah array byte mengandung pattern tertentu */
    private boolean containsBytes(byte[] source, byte[] pattern) {
        if (source == null || pattern == null || source.length < pattern.length) {
            return false;
        }
        for (int i = 0; i <= source.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (source[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    private String generateJwtTokenForUser(Long userId) {
        // Cari username dari database berdasarkan userId, lalu generate JWT
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BiometricAuthenticationException("User not found with id: " + userId));
        return jwtUtil.generateToken(user.getUsername());
    }
}