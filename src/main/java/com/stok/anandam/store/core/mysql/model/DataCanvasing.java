package com.stok.anandam.store.core.mysql.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "data_canvasing")
public class DataCanvasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relasi ke Tabel Canvasing
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canvasing_id", nullable = false)
    private Canvasing canvasing;

    private LocalDate tanggal;

    // Handling ENUM ('canvas', 'visit')
    @Enumerated(EnumType.STRING)
    @Column(name = "canvas_visit")
    private TipeKunjungan canvasVisit;

    private String keterangan;

    @Column(columnDefinition = "TEXT")
    private String catatan;

    // Definisi Enum agar type-safe
    public enum TipeKunjungan {
        canvas,
        visit
    }
}
