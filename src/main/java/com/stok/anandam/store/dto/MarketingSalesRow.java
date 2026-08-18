package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Satu baris ringkasan omzet (dan margin kotor) per marketing (emp_code/emp_name).
 */
@Data
@Builder
public class MarketingSalesRow {
    private String empCode;      // Kode marketing / karyawan (emp_code)
    private String empName;      // Nama marketing (emp_name)
    private BigDecimal qty;      // Total quantity terjual
    private BigDecimal omset;    // Total penjualan (sum grand_total)
    private BigDecimal totalHpp; // Total Harga Pokok Penjualan (sum total_hpp)
    private BigDecimal labaKotor;// Laba kotor = omset - total HPP (sum laba_kotor)
    private BigDecimal marginPct;// Persentase margin kotor
}
