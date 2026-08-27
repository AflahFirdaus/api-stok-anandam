package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryScanRequest {

    @NotBlank(message = "QR Code tidak boleh kosong")
    private String qrCode;
}
