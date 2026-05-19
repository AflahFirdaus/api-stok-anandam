package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import com.stok.anandam.store.core.postgres.model.enums.MemoStatusConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "memos")
@SQLDelete(sql = "UPDATE memos SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Memo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nomor_memo", unique = true)
    private String nomorMemo;

    @Column(name = "kode_pos")
    private String kodePos;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "pelanggan_mybiz_id")
    private PelangganMybiz pelangganMybiz;


    @ManyToOne
    @JoinColumn(name = "marketing_id")
    private User marketing;

    @Column(name = "marketing_emp_code")
    private String marketingEmpCode;

    @Column(name = "marketing_name")
    private String marketingName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    private LocalDateTime tanggalMemo;
    
    @Column(name = "is_teknis_required")
    private Boolean isTeknisRequired;

    @Column(name = "is_delivery_required")
    private Boolean isDeliveryRequired;

    @Column(name = "opsi_pengiriman")
    private String opsiPengiriman;

    @Column(name = "qr_code", columnDefinition = "text")
    private String qrCode;
    
    @jakarta.persistence.Convert(converter = MemoStatusConverter.class)
    @Column(name = "status_akhir")
    private MemoStatus statusAkhir;
    
    private BigDecimal totalHarga;
    
    @Column(columnDefinition = "text")
    private String deskripsi;

    @Column(name = "nomor_jl")
    private String nomorJl;
    
    @Column(columnDefinition = "text")
    private String keteranganGudang;

    private String metodePembayaran;
    
    @Column(name = "memo_type")
    private String memoType;

    @Column(name = "order_id_marketplace")
    private String orderIdMarketplace;

    private String resi;
    private String ekspedisi;
    private String subEkspedisi;
    private String platform;
    private String tempo;
    private String badanUsaha;
    
    @Column(name = "bukti_foto")
    private String buktiFoto;

    @Column(name = "revised_from_id")
    private UUID revisedFromId;

    @Column(name = "revision_to_id")
    private UUID revisionToId;

    // --- AUDIT FIELDS ---

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}