package com.stok.anandam.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;

    public SecurityConfig(UserDetailsService userDetailsService, JwtFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /** Actuator: health & info (Tanpa Auth) */
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /** API (JWT Based) dengan penanganan 401 & 403 yang benar */
    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                .csrf(csrf -> csrf.disable())
                
                // --- PENANGANAN ERROR AUTH/AUTHZ ---
                .exceptionHandling(exception -> exception
                    // Jika token salah/expired/tidak ada (401)
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"code\": \"TOKEN_EXPIRED\", \"message\": \"Sesi habis atau token tidak valid\"}");
                    })
                    // Jika token benar tapi role tidak cukup (403)
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"code\": \"ACCESS_DENIED\", \"message\": \"Anda tidak memiliki izin akses fitur ini\"}");
                    })
                )

                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()

                        // RBAC: ADMIN ONLY
                        .requestMatchers("/api/v1/users/**", "/api/v1/migration/**", "/api/v1/activity-logs/**", 
                                        "/api/v1/old-data/**", "/api/v1/sales/export", "/api/v1/purchases/export").hasRole("ADMIN")
                        .requestMatchers("/api/v1/sales/**", "/api/v1/purchase/**", "/api/v1/purchases/**").hasRole("ADMIN")

                        // RBAC: ADMIN, SPV_MARKETING, MARKETING
                        .requestMatchers("/api/v1/tkdn/**", "/api/v1/canvasing/**", "/api/v1/stock/**", 
                                        "/api/v1/stocks/**", "/api/v1/dashboard/**").hasAnyRole("ADMIN", "SPV_MARKETING", "MARKETING")

                        // RBAC: ADMIN, SPV_MARKETING ONLY
                        .requestMatchers("/api/v1/data-canvasing/**", "/api/sn/**").hasAnyRole("ADMIN", "SPV_MARKETING")

                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /** Web Dashboard (Session/Form Based) */
    @Bean
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/", "/check-db", "/login", "/logout", "/dashboard", "/login.html")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/check-db", "/login", "/logout", "/login.html").permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(login -> login
                        .permitAll()
                        .defaultSuccessUrl("/dashboard", true))
                .logout(logout -> logout.permitAll())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }
}