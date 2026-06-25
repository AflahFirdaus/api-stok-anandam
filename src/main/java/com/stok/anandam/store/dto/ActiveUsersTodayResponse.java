package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO untuk jumlah dan daftar user aktif hari ini.
 * Data diambil dari tabel activity_logs berdasarkan DISTINCT username pada hari berjalan.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveUsersTodayResponse {
    /** Jumlah user unik yang aktif hari ini */
    private long count;

    /** Daftar username unik yang aktif hari ini */
    private List<String> usernames;
}
