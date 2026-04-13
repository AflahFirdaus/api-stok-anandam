package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignRequest {
    @NotNull(message = "Personel ID tidak boleh kosong")
    private Long personelId;
    
    private String alasan;
}
