package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Role;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Cari user di database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan: " + username));

        // 2. Terjemahkan Role (Enum) ke Authority Spring Security
        // Format standar Spring Security untuk role adalah "ROLE_NAMA"
        List<SimpleGrantedAuthority> authorities;
        if (user.getRole() == Role.MANAGER) {
            // MANAGER = peran tertinggi, akses penuh (tanpa batasan).
            // Berikan SEMUA authority role sehingga lolos seluruh gate hasAnyAuthority
            // di SecurityConfig tanpa harus menambahkan ROLE_MANAGER ke tiap daftar role.
            // ROLE_MANAGER ikut termasuk, sehingga endpoint yang khusus ROLE_MANAGER
            // (mis. laporan omset marketing) tetap hanya bisa diakses Manager.
            authorities = Arrays.stream(Role.values())
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .collect(Collectors.toList());
        } else {
            authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        // 3. Kembalikan object User milik Spring Security (bukan User entity kita)
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getActive()), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}