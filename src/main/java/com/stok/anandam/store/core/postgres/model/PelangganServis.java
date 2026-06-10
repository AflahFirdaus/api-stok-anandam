package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pelanggan_servis")
public class PelangganServis {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Hapus name="nama_pelanggan", biarkan Spring Boot yang me-mapping otomatis
    @Column(nullable = false)
    private String namaPelanggan;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private KategoriPelanggan kategori = KategoriPelanggan.User;

    // Hapus name="no_telepon"
    private String noTelepon;

    // Hapus name="no_whatsapp"
    private String noWhatsapp;

    @Column(columnDefinition = "text")
    private String alamat;

    // Untuk kolom yang statis dan tidak menggunakan camelCase, Anda bisa mempertahankannya
    // atau menyesuaikan gaya camelCase seperti createdAt
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}