package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Detail per barang (item) untuk satu marketing pada periode terpilih.
 * Menampilkan ringkasan emiter marketing + list baris per barang + total footer.
 */
@Data
@Builder
public class MarketingItemDetailResponse {

    private String empCode;
    private String empName;
    private String period;          // DAY | WEEK | MONTH | YEAR
    private LocalDate start;
    private LocalDate end;

    private List<MarketingItemRow> content;

    private BigDecimal totalQty;
    private BigDecimal totalOmset;
    private BigDecimal totalHpp;
    private BigDecimal totalLabaKotor;
    private BigDecimal marginPct;
}
