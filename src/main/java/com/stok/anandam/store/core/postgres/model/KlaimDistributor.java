package com.stok.anandam.store.core.postgres.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "klaim_distributor")
public class KlaimDistributor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaksi_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "pelanggan", "penerima", "teknisi", "penyerah"})
    private TransaksiServis transaksi;

    @Column(nullable = false)
    private String namaDistributor;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String alamatDistributor;

    private LocalDateTime tanggalKirim;
    
    private LocalDateTime tanggalKembali;

    private String resiPengiriman;
    
    private BigDecimal biayaKlaim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}