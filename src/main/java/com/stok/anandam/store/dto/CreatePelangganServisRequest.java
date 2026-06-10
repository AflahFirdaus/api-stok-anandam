package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.KategoriPelanggan;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePelangganServisRequest {
    
    @NotBlank(message = "Nama pelanggan tidak boleh kosong")
    private String namaPelanggan;
    
    private KategoriPelanggan kategori;
    
    private String noTelepon;
    
    private String noWhatsapp;
    
    private String alamat;
}
