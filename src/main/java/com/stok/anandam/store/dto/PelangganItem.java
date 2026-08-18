package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Baris pelanggan dari tabel Pelanggan (database eksternal). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PelangganItem {
    private String id;
    private String namaInstansi;
    private String kategori;
    private String kabupaten;
    private String provinsi;
    private String alamat;
}