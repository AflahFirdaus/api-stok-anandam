package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishGudangRequest {
    private Long driverId;
    private Long teknisiId;
    private Long marketingId;
    private String tanggalJadwal; // Format: dd-MM-yyyy
}
