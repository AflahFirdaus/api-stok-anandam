package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryScanResponse {

    private UUID memoId;
    private String nomorMemo;
    private Long penjadwalanId;
    private String alamatLengkap;
    private String alamatMaps;
    private String namaPenerima;
    private String noHpPenerima;
    private String pesan;
}
