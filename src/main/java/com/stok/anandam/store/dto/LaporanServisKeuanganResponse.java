package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class LaporanServisKeuanganResponse {
    private String periode;
    private long totalTransaksiServis;
    private BigDecimal totalOmzetServis;
    private BigDecimal totalModalSparepart;
    private BigDecimal labaBersihServis;
}