package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePenjadwalanRequest {
    
    private Long personelId;
    
    private String tanggalJadwal; // Format: dd-MM-yyyy
    
    private String estimasiWaktu;
    
    private String catatan;
    
    private String alasan; // Untuk log re-assign

    private Double latitude;
    private Double longitude;
}
