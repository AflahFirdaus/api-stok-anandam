package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotBlank;

public final class BiometricDto {
    
    public record RegisterRequest(
        @NotBlank String deviceId,
        String deviceName,
        @NotBlank String publicKey
    ) {}

    public record ChallengeRequest(
        @NotBlank String deviceId
    ) {}

    public record ChallengeResponse(
        String challenge
    ) {}

    public record VerifyRequest(
        @NotBlank String deviceId,
        @NotBlank String challenge,
        @NotBlank String signature
    ) {}

    public record AuthResponse(
        String token,
        String tokenType
    ) {}
}
