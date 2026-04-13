package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.Data; // 1. Tambahkan baris import ini
import java.time.LocalDateTime;
import java.util.UUID;

@Data // 2. Tambahkan tulisan ini di atas @Entity
@Entity
@Table(name = "memo_logs")
public class MemoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Menggunakan UUID sesuai schema tabel memos
    @Column(name = "memo_id", nullable = false)
    private UUID memoId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "aktor_id", nullable = false)
    private Long aktorId;

    @Column(columnDefinition = "TEXT")
    private String keterangan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    public MemoLog() {}
    
    public MemoLog(UUID memoId, String status, Long aktorId, String keterangan) {
        this.memoId = memoId;
        this.status = status;
        this.aktorId = aktorId;
        this.keterangan = keterangan;
    }
}