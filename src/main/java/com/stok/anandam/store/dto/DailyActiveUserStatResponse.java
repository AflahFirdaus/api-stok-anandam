package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO untuk satu titik data statistik harian user aktif.
 * Digunakan untuk membangun grafik tren user aktif per hari.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailyActiveUserStatResponse {
    /** Tanggal (format: yyyy-MM-dd) */
    private String date;

    /** Jumlah user unik yang aktif pada tanggal tersebut */
    private long userCount;
}
