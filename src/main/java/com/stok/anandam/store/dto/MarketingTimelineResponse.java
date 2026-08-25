package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingTimelineResponse {
    private String period;
    private LocalDate start;
    private LocalDate end;
    private String empCode;
    private String empName;
    private List<MarketingTimelinePoint> points;
    private BigDecimal totalOmset;
    private BigDecimal totalHpp;
    private BigDecimal totalLabaKotor;
    private Double marginPct;
}
