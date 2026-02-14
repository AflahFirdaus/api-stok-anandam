package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // Siapa pelakunya
    
    private String action;   // Ngapain? (MIGRASI_TKDN, INPUT_BARANG, dll)
    
    @Column(columnDefinition = "TEXT")
    private String details;  // Detail (Sukses/Gagal, atau ID data yg diubah)
    
    private String ipAddress; // Dari IP mana

    @CreationTimestamp
    private LocalDateTime timestamp;
}