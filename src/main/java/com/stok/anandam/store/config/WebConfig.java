package com.stok.anandam.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Izinkan semua endpoint
                .allowedOrigins(
                        "http://localhost:3000", // React/Next.js default
                        "http://localhost:4200", // Angular default
                        "http://localhost:5173", // Vite/Vue default
                        "*" // ATAU pakai bintang (*) untuk development (Hati-hati saat production!)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Method yang diizinkan
                .allowedHeaders("*") // Header apa saja (termasuk Authorization)
                .allowCredentials(false) // Set true jika pakai Cookies (Jika pakai '*', ini harus false)
                .maxAge(3600); // Cache setting CORS selama 1 jam
    }
}