package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateKlaimRequest {
    @NotBlank(message = "Nama distributor wajib diisi")
    private String namaDistributor;
    
    @NotBlank(message = "Alamat distributor wajib diisi")
    private String alamatDistributor;
    
    private String resiPengiriman;
    
    private BigDecimal biayaKlaim;
}
