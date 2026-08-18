package com.stok.anandam.store.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DataCanvasingRequest {
    // Terima baik "pelangganId" (baru) maupun "canvasingId" (lama) agar
    // aplikasi yang belum diperbarui tetap berjalan.
    @JsonAlias({"pelangganId", "canvasingId"})
    @NotBlank(message = "ID Pelanggan wajib diisi")
    private String pelangganId;

    @NotNull(message = "Tanggal wajib diisi")
    private LocalDate tanggal;

    @NotBlank(message = "Tipe kunjungan (Canvas/Visit) wajib diisi")
    private String canvasVisit;

    private String keterangan;
    private String catatan;
}