package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingTimelinePoint {
    private String key;       // contoh: "2017", "2026-01", "2026-01-15"
    private String label;     // label tampilan di chart: "2017", "Jan", "15/01"
    private BigDecimal omset;
    private BigDecimal totalHpp;
    private BigDecimal labaKotor;
    private Double marginPct;
}
