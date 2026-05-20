package com.stok.anandam.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdatePhoneRequest {
    @NotBlank(message = "Nomor HP wajib diisi")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Nomor HP harus berupa angka dan berdurasi antara 9 hingga 15 karakter")
    private String noHp;
}
