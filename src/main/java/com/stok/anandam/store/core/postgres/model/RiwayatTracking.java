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
@Table(name = "riwayat_tracking")
public class RiwayatTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaksi_id", nullable = false)
    private TransaksiServis transaksi;

    @Column(name = "status_log")
    private StatusServis statusLog;

    @Column(name = "catatan_publik", columnDefinition = "text")
    private String catatanPublik;

    @Column(name = "waktu_update")
    private LocalDateTime waktuUpdate;

    @Column(columnDefinition = "TEXT")
    private String catatanInternal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diupdate_oleh")
    private User diupdateOleh;

    @PrePersist
    protected void onCreate() {
        this.waktuUpdate = LocalDateTime.now();
    }
}