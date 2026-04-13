package com.stok.anandam.store.core.postgres.model;

import com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "request_delivery")
@SQLDelete(sql = "UPDATE request_delivery SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class RequestDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nomor_request", unique = true, nullable = false)
    private String nomorRequest; // REQ-YYMMDD-XXXX

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "alamat_lengkap", columnDefinition = "text")
    private String alamatLengkap;

    @Column(name = "alamat_maps")
    private String alamatMaps;

    @Column(columnDefinition = "text")
    private String keterangan;

    @Column(name = "kode_pos")
    private String kodePos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestDeliveryStatus status;

    @Column(name = "is_urgen")
    @Builder.Default
    private Boolean isUrgen = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne(mappedBy = "requestDelivery", fetch = FetchType.LAZY)
    private PenjadwalanKonfirmasi penjadwalan;
}
