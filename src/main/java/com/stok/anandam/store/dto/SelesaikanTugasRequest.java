package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SelesaikanTugasRequest {
    @NotBlank(message = "Foto bukti wajib dilampirkan")
    private String fotoBukti;
    
    private String catatan; // Catatan tambahan dari teknisi/driver
}
