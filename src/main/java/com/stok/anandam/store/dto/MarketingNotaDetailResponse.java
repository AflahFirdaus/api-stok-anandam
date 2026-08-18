package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Detail transaksi per nota (doc) untuk satu marketing pada periode terpilih.
 * Menampilkan ringkasan emiter marketing + list baris per nota + total footer.
 */
@Data
@Builder
public class MarketingNotaDetailResponse {

    private String empCode;
    private String empName;
    private String period;          // DAY | WEEK | MONTH | YEAR
    private LocalDate start;
    private LocalDate end;

    private List<MarketingNotaRow> content;

    private BigDecimal totalQty;
    private BigDecimal totalOmset;
    private BigDecimal totalHpp;
    private BigDecimal totalLabaKotor;
    private BigDecimal marginPct;
}
