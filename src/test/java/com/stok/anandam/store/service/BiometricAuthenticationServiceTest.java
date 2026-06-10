package com.stok.anandam.store.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stok.anandam.store.core.postgres.model.UserBiometricCredential;
import com.stok.anandam.store.core.postgres.repository.UserBiometricCredentialRepository;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.BiometricDto.ChallengeRequest;
import com.stok.anandam.store.dto.BiometricDto.ChallengeResponse;
import com.stok.anandam.store.exception.GlobalExceptionHandler.BiometricAuthenticationException;
import com.stok.anandam.store.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
public class BiometricAuthenticationServiceTest {

    @Mock
    private UserBiometricCredentialRepository credentialRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private BiometricAuthenticationService biometricService;

    private String deviceId = "test-device-id";

    @Test
    public void testGenerateChallenge_DeviceRegistered() {
        UserBiometricCredential credential = new UserBiometricCredential();
        credential.setDeviceId(deviceId);
        credential.setActive(true);

        when(credentialRepository.findByDeviceIdAndActiveTrue(deviceId))
                .thenReturn(Optional.of(credential));

        ChallengeRequest request = new ChallengeRequest(deviceId);
        ChallengeResponse response = biometricService.generateChallenge(request);

        assertNotNull(response);
        assertNotNull(response.challenge());
        assertFalse(response.challenge().isEmpty());
    }

    @Test
    public void testGenerateChallenge_DeviceNotRegistered() {
        when(credentialRepository.findByDeviceIdAndActiveTrue(deviceId))
                .thenReturn(Optional.empty());

        ChallengeRequest request = new ChallengeRequest(deviceId);

        assertThrows(BiometricAuthenticationException.class, () -> {
            biometricService.generateChallenge(request);
        });
    }
}
