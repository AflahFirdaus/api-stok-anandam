package com.stok.anandam.store.dto;

import lombok.Data;

@Data
public class CreatePengambilanRequest {
    
    // Opsional: Jika terkait memo tertentu
    private java.util.UUID memoId;

    // Lokasi pengambilan
    private String alamatLengkap;
    private String alamatMaps;
    private Integer idKodepos;

    // Jadwal
    private String tanggalJadwal;   // Format: "2026-03-15"
    private String estimasiWaktu;   // Contoh: "10.00 - 12.00"

    // Info tambahan
    private String catatan;
    private Long aktorId;           // ID user yang request
}
