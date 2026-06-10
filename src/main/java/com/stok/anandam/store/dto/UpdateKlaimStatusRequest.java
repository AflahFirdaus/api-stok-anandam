package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.StatusServis;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateKlaimStatusRequest {
    @NotNull(message = "Status baru wajib ditentukan")
    private StatusServis statusBaru;
    
    private String catatanInternal;
    
    private String catatanPublik;
    
    private String resiPengiriman; // Diisi jika ada pembaruan nomor resi kurir
}