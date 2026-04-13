package com.stok.anandam.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KonfirmasiKirimRequest {
    
    // @NotEmpty memastikan list tidak null dan minimal memiliki 1 elemen
    @NotEmpty(message = "Daftar item yang dikirim tidak boleh kosong")
    // @Valid sangat penting! Ini menyuruh Spring untuk juga memvalidasi isi di dalam ItemKirimDTO
    @Valid
    @Builder.Default
    private List<ItemKirim> items = new ArrayList<>();
}
