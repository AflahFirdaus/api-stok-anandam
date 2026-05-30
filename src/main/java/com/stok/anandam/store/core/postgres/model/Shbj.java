package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shbj")
@Data
public class Shbj {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uraian_kelompok_barang", length = 86, nullable = false)
    private String uraianKelompokBarang;

    @Column(name = "uraian_barang", length = 136)
    private String uraianBarang;

    @Column(name = "spesifikasi", length = 627)
    private String spesifikasi;

    @Column(name = "satuan", length = 19)
    private String satuan;

    @Column(name = "harga_satuan", length = 14)
    private String hargaSatuan;
}