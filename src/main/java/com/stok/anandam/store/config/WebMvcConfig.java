package com.stok.anandam.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /download/** url pattern to physical directory /var/www/app-storage/
        // If not using Linux, you can conditionally check OS or use application.properties.
        // For production, the user requested /var/www/app-storage/
        
        String storagePath = "file:/var/www/app-storage/";
        
        // Buat folder jika tidak ada saat dev (untuk menghindari error di local Windows)
        File f = new File("/var/www/app-storage/");
        if (!f.exists()) {
            // Biarkan saja, Spring tetap jalan tapi endpoint tidak menemukan file.
            // Di production Linux folder ini harus disiapkan sesuai panduan.
        }

        registry.addResourceHandler("/download/**")
                .addResourceLocations(storagePath);
    }
}
