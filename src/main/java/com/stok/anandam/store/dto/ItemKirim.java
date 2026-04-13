package com.stok.anandam.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// --- DTO untuk Konfirmasi Kirim (Per Item) ---
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemKirim {
    
    @NotNull(message = "ID Item tidak boleh kosong")
    private Long itemId;

    @NotNull(message = "Quantity (jumlah) dikirim tidak boleh kosong")
    @Min(value = 1, message = "Quantity yang dikirim minimal harus 1")
    private Integer qtyDikirimSaatIni;
}
