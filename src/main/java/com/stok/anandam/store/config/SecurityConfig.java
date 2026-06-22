package com.stok.anandam.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

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

        /** Actuator: health & info (Tanpa Auth), lainnya butuh ADMIN */
        @Bean
        public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/actuator/**")
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                                .anyRequest().hasRole("ADMIN"))
                                .csrf(csrf -> csrf.disable());
                return http.build();
        }

        /** API (JWT Based) dengan penanganan 401 & 403 yang benar */
        @Bean
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())

                                // --- PENANGANAN ERROR AUTH/AUTHZ ---
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                        "{\"code\": \"TOKEN_EXPIRED\", \"message\": \"Sesi habis atau token tidak valid\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write(
                                                                        "{\"code\": \"ACCESS_DENIED\", \"message\": \"Anda tidak memiliki izin akses fitur ini\"}");
                                                }))

                                .authorizeHttpRequests(auth -> auth
                                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                                        .requestMatchers("/api/v1/biometric/challenge", "/api/v1/biometric/verify").permitAll()
                                        .requestMatchers("/api/v1/public/app/latest").permitAll()
                                        .requestMatchers("/api/v1/public/tracking/**").permitAll()
                                        .requestMatchers("/api/v1/internal/**").permitAll() // Secured by ApiKeyFilter
                                        .requestMatchers("/download/**").permitAll()
                                        .requestMatchers("/ws/**", "/ws-connect/**").permitAll()
                                        
                                        // REVISI: Izinkan semua user yang login untuk akses log sinkronisasi & profil sendiri
                                        .requestMatchers("/api/v1/activity-logs/last-sync").authenticated()
                                        .requestMatchers("/api/v1/users/me", "/api/v1/users/profile", "/api/v1/kodepos").authenticated() 

                                        // PRINTER: Wajib login (membawa token) agar tidak bisa print spam dari luar
                                        .requestMatchers("/api/v1/printer/**").authenticated()

                                        // REVISI: Pastikan endpoint stok bisa diakses operasional
                                        .requestMatchers("/api/v1/stock", "/api/v1/stock/**", "/api/v1/stocks", "/api/v1/stocks/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_GUDANG", "ROLE_DELIVERY", "ROLE_TEKNISI", "ROLE_NOTA")
                                        .requestMatchers("/api/sn", "/api/sn/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_GUDANG", "ROLE_TEKNISI", "ROLE_DELIVERY", "ROLE_NOTA")

                                        // PENJADWALAN
                                        .requestMatchers(HttpMethod.GET, "/api/v1/penjadwalan", "/api/v1/penjadwalan/**").authenticated()
                                        .requestMatchers(HttpMethod.PUT, "/api/v1/penjadwalan/*/reassign", "/api/v1/penjadwalan/*/reassign/").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_GUDANG")
                                        .requestMatchers(HttpMethod.POST, "/api/v1/penjadwalan", "/api/v1/penjadwalan/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_GUDANG", "ROLE_TEKNISI", "ROLE_DELIVERY")
                                        .requestMatchers(HttpMethod.PUT, "/api/v1/penjadwalan", "/api/v1/penjadwalan/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_GUDANG", "ROLE_TEKNISI", "ROLE_DELIVERY")

                                        // MEMOS (Strict RBAC)
                                        .requestMatchers(HttpMethod.POST, "/api/v1/memos/*/penjadwalan", "/api/v1/memos/*/konfirmasi-kirim").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_GUDANG", "ROLE_TEKNISI", "ROLE_DELIVERY")
                                        .requestMatchers(HttpMethod.POST, "/api/v1/memos", "/api/v1/memos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE")
                                        .requestMatchers(HttpMethod.PUT, "/api/v1/memos/*/gudang-finish", "/api/v1/memos/*/invoice-finish", "/api/v1/memos/*/technician-finish", "/api/v1/memos/*/delivery-finish", "/api/v1/memos/*/pickup-route", "/api/v1/memos/*/pickup-final", "/api/v1/memos/*/delivery-route", "/api/v1/memos/*/complete").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_GUDANG", "ROLE_TEKNISI", "ROLE_DELIVERY", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_NOTA")
                                        .requestMatchers(HttpMethod.PUT, "/api/v1/memos", "/api/v1/memos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_MARKETING", "ROLE_MARKETING_TOKO", "ROLE_MARKETING_PROJECT", "ROLE_MARKETING_DISTRIBUSI", "ROLE_MARKETING_ONLINE", "ROLE_GUDANG", "ROLE_DELIVERY", "ROLE_TEKNISI", "ROLE_NOTA")
                                        .requestMatchers(HttpMethod.GET, "/api/v1/memos", "/api/v1/memos/**").authenticated()
                                        
                                        // REQUEST DELIVERY (New System)
                                        .requestMatchers(HttpMethod.GET, "/api/v1/request-delivery", "/api/v1/request-delivery/", "/api/v1/request-delivery/**").authenticated()
                                        .requestMatchers(HttpMethod.POST, "/api/v1/request-delivery", "/api/v1/request-delivery/", "/api/v1/request-delivery/**").authenticated()


                                        // Endpoint Admin & Spv (Data sensitif)
                                        .requestMatchers(HttpMethod.GET, "/api/v1/users", "/api/v1/users/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_GUDANG", "ROLE_MARKETING", "ROLE_MARKETING_ONLINE", "ROLE_TEKNISI", "ROLE_DELIVERY")
.requestMatchers("/api/v1/users/**", "/api/v1/activity-logs/**", "/api/v1/old-data/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI", "ROLE_MARKETING_ONLINE", "ROLE_TEKNISI")
                                        .requestMatchers("/api/v1/sales/export", "/api/v1/purchases/export").hasAnyAuthority("ROLE_ADMIN", "ROLE_SPV_MARKETING", "ROLE_SPV_GUDANG", "ROLE_SPV_TEKNISI")

                                        // Catch-all sisanya
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
                                .cors(Customizer.withDefaults())
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

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // Izinkan domain production dan localhost untuk development
                configuration.setAllowedOrigins(List.of(
                    "https://api.anandamcomputer.com",
                    "https://admin.anandamcomputer.com",
                    "https://anandam.id",
                    "http://localhost",
                    "http://localhost:5173",
                    "http://localhost:3000",
                    "http://127.0.0.1:5173",
                    "http://127.0.0.1:3000"
                ));
                
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                
                configuration.setAllowedHeaders(List.of("*"));
                
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration); 
                return source;
        }
}