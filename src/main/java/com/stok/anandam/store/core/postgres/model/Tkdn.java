package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "tkdn")
public class Tkdn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String kategori;

    private Integer modal;

    private Integer dealer;
    private Integer principal;
    private Integer tayang;

    @Column(name = "sertifikat_tkd", length = 100)
    private String sertifikatTkd;

    @Column(precision = 5, scale = 2)
    private BigDecimal presentase;

    @Column(name = "no_merek", length = 100)
    private String noMerek;

    private String nama;

    @Column(columnDefinition = "TEXT")
    private String spesifikasi;

    @Column(name = "distri")
    private Integer distri;

    @Column(name = "processor", length = 255)
    private String processor;

    @Column(name = "ram", length = 255)
    private String ram;

    @Column(name = "ssd", length = 255)
    private String ssd;

    @Column(name = "hdd", length = 255)
    private String hdd;

    @Column(name = "vga", length = 255)
    private String vga;

    @Column(name = "layar", length = 255)
    private String layar;

    @Column(name = "os", length = 255)
    private String os;

    @Column(name = "garansi", length = 100)
    private String garansi;
}