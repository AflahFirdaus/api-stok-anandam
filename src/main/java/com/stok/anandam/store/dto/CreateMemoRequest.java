package com.stok.anandam.store.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.gson.annotations.SerializedName;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemoRequest {
    // 1. Data dari UI Kiri
    @JsonProperty("namaCustomer")
    @JsonAlias({"nama_customer", "namaCustomer"})
    @SerializedName("namaCustomer")
    private String namaCustomer;
    
    @JsonProperty("tanggal")
    @JsonAlias({"tanggal", "tanggal_memo"})
    @SerializedName("tanggal")
    private String tanggal;     // Contoh: "06/03/2026"
    @JsonProperty("deskripsi")
    @JsonAlias("deskripsi")
    @SerializedName("deskripsi")
    private String deskripsi;
    
    // 2. Data dari UI Kanan
    @JsonProperty("noHpCustomer")
    @JsonAlias({"no_hp_customer", "noHpCustomer"})
    @SerializedName("noHpCustomer")
    private String noHpCustomer;
    
    @JsonProperty("namaMarketing")
    @JsonAlias({"nama_marketing", "namaMarketing"})
    @SerializedName("namaMarketing")
    private String namaMarketing;

    @JsonProperty("marketingEmpCode")
    @JsonAlias({"marketing_emp_code", "marketingEmpCode"})
    @SerializedName("marketingEmpCode")
    private String marketingEmpCode;
    
    // Proses (Radio Button / Toggle)
    @JsonProperty("isTeknisi")
    @JsonAlias({"is_teknisi", "isTeknisi"})
    @SerializedName("isTeknisi")
    private Boolean isTeknisi;
    
    @JsonProperty("isKirim")
    @JsonAlias({"is_kirim", "isKirim"})
    @SerializedName("isKirim")
    private Boolean isKirim;
    
    // Dropdown dan Payment
    @JsonProperty("opsiPengiriman")
    @JsonAlias({"opsi_pengiriman", "opsiPengiriman"})
    @SerializedName("opsiPengiriman")
    private String opsiPengiriman; 
    
    @JsonProperty("metodePembayaran")
    @JsonAlias({"metode_pembayaran", "metodePembayaran"})
    @SerializedName("metodePembayaran")
    private String metodePembayaran; 

    @JsonProperty("totalHarga")
    @JsonAlias({"total_harga", "totalHarga"})
    @SerializedName("totalHarga")
    private BigDecimal totalHarga;

    @JsonProperty("memoType")
    @JsonAlias({"memo_type", "memoType"})
    @SerializedName("memoType")
    private String memoType;
    
    @JsonProperty("orderIdMarketplace")
    @JsonAlias({"order_id_marketplace", "orderIdMarketplace"})
    @SerializedName("orderIdMarketplace")
    private String orderIdMarketplace;
    
    @JsonProperty("resi")
    @JsonAlias("resi")
    @SerializedName("resi")
    private String resi;
    
    @JsonProperty("ekspedisi")
    @JsonAlias("ekspedisi")
    @SerializedName("ekspedisi")
    private String ekspedisi;
    
    @JsonProperty("platform")
    @JsonAlias("platform")
    @SerializedName("platform")
    private String platform;

    @JsonProperty("kodePos")
    @JsonAlias({"kode_pos", "kodePos", "kodepos"})
    @SerializedName("kodePos")
    private String kodePos;

    @JsonProperty("tempo")
    @JsonAlias("tempo")
    @SerializedName("tempo")
    private String tempo;

    // 3. Data Barang
    @JsonProperty("items")
    @JsonAlias("items")
    @SerializedName("items")
    private List<MemoItemRequest> items;
}