package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.enums.TipeTugas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateManualJadwalRequest {

    @NotBlank(message = "Nama User tidak boleh kosong")
    private String userName;

    private String noHp;

    @NotBlank(message = "Marketing Request tidak boleh kosong")
    private String marketingName;

    @NotBlank(message = "Tanggal Rencana tidak boleh kosong")
    private String tanggalJadwal; // Format: "dd-MM-yyyy"

    @NotBlank(message = "Estimasi Waktu tidak boleh kosong")
    private String estimasiWaktu;

    private Integer idKodepos;

    @NotBlank(message = "Alamat Pengiriman tidak boleh kosong")
    private String alamatLengkap;

    private String alamatMaps;

    private String catatan;

    @NotNull(message = "Tipe Tugas harus dipilih")
    private TipeTugas tipeTugas; // PENGIRIMAN / PENGAMBILAN

    @Builder.Default
    private Boolean isUrgen = false;
}
