package com.stok.anandam.store.controller;

import com.stok.anandam.store.annotation.LogActivity;
import com.stok.anandam.store.core.postgres.model.RefreshToken;
import com.stok.anandam.store.dto.*;
import com.stok.anandam.store.service.AuthService;
import com.stok.anandam.store.service.RefreshTokenService;
import com.stok.anandam.store.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtUtil jwtUtil;

    // 1. LOGIN (Dapat 2 Token)
    @PostMapping("/login")
    @LogActivity("USER LOGIN KE SYSTEM")
    public ResponseEntity<WebResponse<TokenResponse>> login(@Valid @RequestBody LoginUserRequest request) {
        // Autentikasi User (Cek password dll di AuthService)
        authService.authenticate(request); 

        // Generate Access Token (Pendek)
        String accessToken = jwtUtil.generateToken(request.getUsername());

        // Generate Refresh Token (Panjang & Masuk DB)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type("Bearer")
                .build();

        WebResponse<TokenResponse> response = WebResponse.<TokenResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login Berhasil")
                .data(tokenResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    // 2. REFRESH TOKEN (Tukar Token Lama -> Baru)
    @PostMapping("/refresh")
    public ResponseEntity<WebResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration) // Cek expired
                .map(RefreshToken::getUser) // Ambil user-nya
                .map(user -> {
                    // Bikin Access Token Baru
                    String accessToken = jwtUtil.generateToken(user.getUsername());
                    
                    TokenResponse tokenResponse = TokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(requestRefreshToken) // Balikin refresh token yg sama (atau bisa diputar)
                            .type("Bearer")
                            .build();
                            
                    return ResponseEntity.ok(WebResponse.<TokenResponse>builder()
                            .status(200).message("Token Refreshed").data(tokenResponse).build());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}