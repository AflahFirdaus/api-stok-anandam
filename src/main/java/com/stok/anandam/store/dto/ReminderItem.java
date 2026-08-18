package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data Reminder yang sudah diolah agar siap dibaca user. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderItem {
    private String id;
    private String namaInstansi;
    private String kategori;
    private String kabupaten;
    /** Tanggal kunjungan terakhir format Indonesia, mis. "12 Agustus 2026". */
    private String lastCanvasAtLabel;
    /** Berapa hari sejak terakhir dikunjungi. */
    private long daysNotVisited;
    /** Interval reminder dalam hari (snapshot). */
    private int intervalDays;
    /** Berapa hari melewati batas (daysNotVisited - intervalDays, minimal 0). */
    private long overdueDays;
}