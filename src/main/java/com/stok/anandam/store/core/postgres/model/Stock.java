package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "stok")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stok_seq_gen")
    @SequenceGenerator(name = "stok_seq_gen", sequenceName = "stok_seq", allocationSize = 50)
    private Long id;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "item_name", length = 500)
    private String itemName;

    @Column(name = "normalized_item_name", length = 500)
    private String normalizedItemName;

    // Tambahan kolom baru (walau di query lama tidak ada, kita siapkan null dulu
    // atau diisi logic lain)
    @Column(name = "kategori_nama", length = 50)
    private String kategoriNama;

    @Column(name = "kategori_itemcode", length = 50)
    private String kategoriItemcode;

    @Column(name = "final_stok")
    private Integer finalStok;

    @Column(name = "harga_hpp")
    private BigDecimal hargaHpp;

    @Column(name = "grand_total")
    private BigDecimal grandTotal;

    @Column(name = "warehouse", length = 100)
    private String warehouse;

    @Transient
    private LocalDate lastSalesDate;

    // These fields are populated via lookup to Pricelist table
    @Transient
    private String spesifikasi;

    @Transient
    private java.math.BigDecimal modal;

    @Transient
    private java.math.BigDecimal finalPricelist;
}