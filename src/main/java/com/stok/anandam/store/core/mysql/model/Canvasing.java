package com.stok.anandam.store.core.mysql.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@SuppressWarnings("unused")
@Data
@Entity
@Table(name = "canvasing")
public class Canvasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50)
    private String kategori;

    @Column(name = "nama_instansi")
    private String namaInstansi;

    @Column(length = 100)
    private String provinsi;

    @Column(length = 100)
    private String kabupaten;

    @Column(length = 100)
    private String kecamatan;

    // Opsional: Jika ingin menarik history data canvasing dari parent
    // @OneToMany(mappedBy = "canvasing", cascade = CascadeType.ALL)
    // private List<DataCanvasing> riwayatKunjungan;
}
