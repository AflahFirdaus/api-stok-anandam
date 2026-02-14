package com.stok.anandam.store.core.mysql.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "tkdn")
public class Tkdn { // Ubah 'tkdn' jadi 'Tkdn'

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String kategori;

    private Integer modal;

    private String dealer;
    private String principal;
    private LocalDate tayang;

    @Column(name = "sertifikat_tkd", length = 100)
    private String sertifikatTkd;

    @Column(precision = 5, scale = 2)
    private BigDecimal presentase;

    @Column(name = "no_merek", length = 100)
    private String noMerek; // Kita anggap ini sebagai referensi kode/merk

    private String nama;

    @Column(columnDefinition = "TEXT")
    private String spesifikasi;

    private String distri;

    @Column(length = 100)
    private String processor;

    @Column(length = 50)
    private String ram;

    @Column(length = 50)
    private String ssd;

    @Column(length = 50)
    private String hdd;

    @Column(length = 100)
    private String vga;

    @Column(length = 100)
    private String layar;

    @Column(length = 50)
    private String os;

    @Column(length = 100)
    private String garansi;
}
