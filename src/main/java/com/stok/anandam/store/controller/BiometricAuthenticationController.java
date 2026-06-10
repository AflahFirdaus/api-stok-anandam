package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.BiometricDto.AuthResponse;
import com.stok.anandam.store.dto.BiometricDto.ChallengeRequest;
import com.stok.anandam.store.dto.BiometricDto.ChallengeResponse;
import com.stok.anandam.store.dto.BiometricDto.RegisterRequest;
import com.stok.anandam.store.dto.BiometricDto.VerifyRequest;
import com.stok.anandam.store.service.BiometricAuthenticationService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/biometric")
public class BiometricAuthenticationController {

    private final BiometricAuthenticationService biometricService;
    private final UserRepository userRepository;

    public BiometricAuthenticationController(BiometricAuthenticationService biometricService, UserRepository userRepository) {
        this.biometricService = biometricService;
        this.userRepository = userRepository;
    }

    /**
     * Endpoint ini wajib melewati filter keamanan JWT Anda (Authenticated).
     * User mendaftarkan Public Key perangkat setelah login biasa.
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerBiometric(@Valid @RequestBody RegisterRequest request, Principal principal) {
        // Dapatkan User ID dari database berdasarkan username dari Principal JWT
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        biometricService.registerBiometric(user.getId(), request);
        return ResponseEntity.ok("Biometric public key registered successfully");
    }

    /**
     * Endpoint Publik. Langkah pertama proses login biometrik.
     */
    @PostMapping("/challenge")
    public ResponseEntity<ChallengeResponse> getChallenge(@Valid @RequestBody ChallengeRequest request) {
        ChallengeResponse response = biometricService.generateChallenge(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint Publik. Langkah kedua proses login biometrik untuk menukar signature dengan JWT baru.
     */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyBiometric(@Valid @RequestBody VerifyRequest request) {
        AuthResponse response = biometricService.verifyAndAuthenticate(request);
        return ResponseEntity.ok(response);
    }
}