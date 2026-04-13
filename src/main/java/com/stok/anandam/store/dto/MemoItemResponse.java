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
public class MemoItemResponse {
    private Long id;
    private String namaBarang;
    private Integer qty;
    private BigDecimal hargaSatuan;
    private BigDecimal subtotal;
    private Integer qtyShipped;
    private Integer qtyRemaining;
    private String catatanGudang;
    private String status;
}