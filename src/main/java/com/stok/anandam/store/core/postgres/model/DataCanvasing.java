package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "data_canvasing")
public class DataCanvasing {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "data_canvasing_seq_gen")
    @SequenceGenerator(name = "data_canvasing_seq_gen", sequenceName = "data_canvasing_seq", allocationSize = 50)
    private Long id;

    // Relasi ke Master Canvasing (Toko/Instansi)
    @ManyToOne
    @JoinColumn(name = "canvasing_id", nullable = false)
    private Canvasing canvasing;

    @Column(name = "tanggal")
    private LocalDate tanggal;

    @Column(name = "canvas_visit") // Jenis kunjungan: "Canvas" atau "Visit"
    private String canvasVisit;

    @Column(name = "keterangan", columnDefinition = "TEXT")
    private String keterangan;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;
}