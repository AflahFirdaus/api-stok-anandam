package com.stok.anandam.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemoStatusRequest {
    
    @NotNull(message = "Status tujuan tidak boleh kosong")
    @JsonProperty("targetStatus")
    @JsonAlias({"target_status", "targetStatus"})
    private MemoStatus targetStatus;
    
    @JsonProperty("rolePelaku")
    @JsonAlias({"role_pelaku", "rolePelaku"})
    private String rolePelaku;

    // Opsional: Keterangan log atau catatan untuk penjadwalan
    @JsonProperty("keteranganLog")
    @JsonAlias({"keterangan_log", "keteranganLog", "catatan"})
    private String keteranganLog; 

    @JsonProperty("catatanPenjadwalan")
    @JsonAlias({"catatan_penjadwalan", "catatanPenjadwalan"})
    private String catatanPenjadwalan; 

    private String verificationMethod; // SCAN atau MANUAL
}