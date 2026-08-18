package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Opsi pelanggan untuk dropdown/autocomplete (dari tabel Pelanggan eksternal). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PelangganOption {
    private String id;
    private String namaInstansi;
}