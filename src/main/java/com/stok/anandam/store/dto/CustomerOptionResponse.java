package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerOptionResponse {
    private Long id;
    private String namaPelanggan;
    private String noHp;
    private String source; // "LOCAL" or "MYBIZ"
    
    // Additional fields from PelangganMybiz to auto-populate the form
    private String kodePartner;
    private String kodeMarketing;
    private String namaMarketing;
    private BigDecimal limitPiutang;
    private Integer terminPiutang;
    private BigDecimal limitHutang;
    private Integer terminHutang;
    private String npwp;
    private String alamat;
}
