package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Satu record kunjungan/canvasing dari tabel Canvas (database eksternal), sudah di-JOIN nama instansi. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanvasVisitRecord {
    private String id;
    private String pelangganId;
    private String namaInstansi;
    /** Tanggal kunjungan format Indonesia, mis. "12 Agustus 2026". */
    private String tanggalLabel;
    /** VISIT atau CANVAS. */
    private String kunjungan;
    private String keterangan;
    private String catatan;
}