package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class EmployeeSalesResponse {
    private String empName;
    private String empCode;
    private BigDecimal totalSales;
}
