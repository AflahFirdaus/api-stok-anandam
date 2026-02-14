package com.stok.anandam.store.config;

import com.stok.anandam.store.service.CustomUserDetailsService;
import com.stok.anandam.store.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        // 1. Cek Header Authorization
        // Format harus: "Bearer eyJhbGciOiJIUzI1NiJ9..."
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // Potong kata "Bearer "
            try {
                username = jwtUtil.extractUsername(jwt); // Ambil username dari token
            } catch (Exception e) {
                log.debug("JWT parse error: {}", e.getMessage());
            }
        }

        // 2. Validasi Token & Set Authentication
        // Jika username ketemu DAN belum ada yang login di context saat ini
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Ambil data user dari database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Cek apakah token valid (signature benar & belum expired)
            if (jwtUtil.validateToken(jwt, userDetails)) {
                
                // Buat objek Authentication
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Masukkan user ke context Spring Security (User dianggap LOGIN)
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 3. Lanjut ke filter berikutnya (atau ke Controller)
        filterChain.doFilter(request, response);
    }
}