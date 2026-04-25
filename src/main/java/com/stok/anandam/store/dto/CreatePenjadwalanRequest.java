package com.stok.anandam.store.dto;

import lombok.Data;

@Data
public class CreatePenjadwalanRequest {
    private String tipeTugas;
    private String tanggalJadwal;
    private String catatan;
    
    // Alamat & Lokasi
    private Integer idKodepos;
    private String alamatLengkap;
    private String alamatMaps;
    private String estimasiWaktu;
    private Double latitude;
    private Double longitude;

    private String kabupatenKota;
    private String kecamatan;
    private String desaKelurahan;
}