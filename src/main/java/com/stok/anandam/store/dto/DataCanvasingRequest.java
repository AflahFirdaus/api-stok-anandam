package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DataCanvasingRequest {
    @NotBlank(message = "ID Pelanggan wajib diisi")
    private String pelangganId;

    @NotNull(message = "Tanggal wajib diisi")
    private LocalDate tanggal;

    @NotBlank(message = "Tipe kunjungan (Canvas/Visit) wajib diisi")
    private String canvasVisit;

    private String keterangan;
    private String catatan;
}