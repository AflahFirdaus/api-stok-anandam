package com.stok.anandam.store.core.postgres.model;

import com.stok.anandam.store.core.postgres.model.enums.ItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "memo_items")
public class MemoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @Column(nullable = false)
    private String namaBarang;

    @Column(nullable = false)
    private Integer qty;

    @Column(nullable = false)
    private BigDecimal hargaSatuan;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Builder.Default
    @Column(name = "qty_shipped", nullable = false, columnDefinition = "integer default 0")
    private Integer qtyShipped = 0;

    @Column(name = "catatan_gudang", columnDefinition = "text")
    private String catatanGudang;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", length = 20)
    private ItemStatus itemStatus;

    @Column(length = 50)
    private String status;
    public Integer getSisaQty() {
        return this.qty - this.qtyShipped;
    }

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