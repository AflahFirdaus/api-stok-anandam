package com.stok.anandam.store.service;


import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.LoginUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    // Method ini hanya bertugas memverifikasi password
    public void authenticate(LoginUserRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Update status and device count
            userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
                user.setLastLogin(LocalDateTime.now());
                user.setLastActivity(LocalDateTime.now());
                user.setIsOnline(true);
                user.setDeviceCount((user.getDeviceCount() == null ? 0 : user.getDeviceCount()) + 1);
                userRepository.save(user);
            });
        } catch (DisabledException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Akun Anda telah dinonaktifkan. Silakan hubungi admin.");}}
    public void logout() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByUsername(username).ifPresent(user -> {
            int currentCount = user.getDeviceCount() == null ? 0 : user.getDeviceCount();
            user.setDeviceCount(Math.max(0, currentCount - 1));
            if (user.getDeviceCount() == 0) {
                user.setIsOnline(false);
            }
            userRepository.save(user);
        });
    }
}