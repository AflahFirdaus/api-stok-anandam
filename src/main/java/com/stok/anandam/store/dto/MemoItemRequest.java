package com.stok.anandam.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.gson.annotations.SerializedName;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoItemRequest {
    @JsonProperty("namaBarang")
    @JsonAlias({"nama_barang", "namaBarang"})
    @SerializedName("namaBarang")
    @NotBlank(message = "Nama barang tidak boleh kosong")
    private String namaBarang;
    
    @JsonProperty("qty")
    @JsonAlias("qty")
    @SerializedName("qty")
    @NotNull(message = "Qty tidak boleh kosong")
    @Min(value = 1, message = "Qty minimal 1")
    private Integer qty;
    
    @JsonProperty("hargaSatuan")
    @JsonAlias({"harga_satuan", "hargaSatuan"})
    @SerializedName("hargaSatuan")
    @NotNull(message = "Harga satuan tidak boleh kosong")
    private BigDecimal hargaSatuan;
    
    @JsonProperty("subtotal")
    @JsonAlias("subtotal")
    @SerializedName("subtotal")
    @NotNull(message = "Subtotal tidak boleh kosong")
    private BigDecimal subtotal;

    @JsonProperty("catatan")
    @JsonAlias("catatan")
    @SerializedName("catatan")
    private String catatan;
}