package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

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
        String roleName = "ROLE_" + user.getRole().name(); 
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        // 3. Kembalikan object User milik Spring Security (bukan User entity kita)
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority) // Masukkan role ke dalam list
        );
    }
}