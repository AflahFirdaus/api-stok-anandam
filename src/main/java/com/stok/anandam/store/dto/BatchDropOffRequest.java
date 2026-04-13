package com.stok.anandam.store.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BatchDropOffRequest {
    private List<UUID> memoIds;
    private List<Long> requestDeliveryIds;
    private Long personelId;
    private String tanggalJadwal;
    private String estimasiWaktu;
    private String catatan;
    
    // Lokasi Gerai Ekspedisi (Drop-off Point)
    private Integer idKodepos;
    private String alamatLengkap;
    private String alamatMaps;
    private String manualCustomerName; // Nama Gerai (misal: JNE Agen 001)
}
