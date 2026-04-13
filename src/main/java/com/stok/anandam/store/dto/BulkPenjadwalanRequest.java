package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BulkPenjadwalanRequest {
    private List<String> memoIds;
    private List<Long> requestDeliveryIds;
    
    @NotNull(message = "Personel ID tidak boleh kosong")
    private Long personelId;
    
    @NotNull(message = "Tanggal Rencana tidak boleh kosong")
    private String tanggalRencana; // "yyyy-MM-dd'T'HH:mm:ss.SSS" dari flutter toIso8601String() atau "dd-MM-yyyy"
}
