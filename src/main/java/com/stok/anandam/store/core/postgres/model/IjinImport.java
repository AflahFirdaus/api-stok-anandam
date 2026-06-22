package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ijin_import", schema = "public")
@Data
public class IjinImport {
    @Id
    @Column(name = "no")
    private Integer no;

    @Column(name = "nama_barang")
    private String namaBarang;

    @Column(name = "spesifikasi", columnDefinition = "TEXT")
    private String spesifikasi;

    @Column(name = "keterangan")
    private String keterangan;
}