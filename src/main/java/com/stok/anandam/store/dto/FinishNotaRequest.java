package com.stok.anandam.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishNotaRequest {
    
    @NotBlank(message = "Nomor JL tidak boleh kosong")
    @Pattern(regexp = "^JL-.*", message = "Format JL tidak valid (harus diawali 'JL-')")
    private String nomorJl;

    @JsonProperty("keteranganLog")
    private String keteranganLog;
}
