package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "canvasing")
public class Canvasing {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "canvasing_seq_gen")
    @SequenceGenerator(name = "canvasing_seq_gen", sequenceName = "canvasing_seq", allocationSize = 50)
    private Long id;

    @Column(name = "kategori", length = 50)
    private String kategori;

    @Column(name = "nama_instansi")
    private String namaInstansi;

    @Column(name = "provinsi", length = 100)
    private String provinsi;

    @Column(name = "kabupaten", length = 100)
    private String kabupaten;

    @Column(name = "kecamatan", length = 100)
    private String kecamatan;
}