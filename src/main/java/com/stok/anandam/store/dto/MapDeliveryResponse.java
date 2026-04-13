package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapDeliveryResponse {
    private String idMemo;
    private String nomorMemo;
    private String customerName;
    private String desa;
    private String kecamatan;
    private String kabupaten;
    private String kodePos;
    private BigDecimal lat;
    private BigDecimal lng;
    private String status;
    private String memoStatus;
    private String mapUrl;
    private String senderName;
    private Boolean isUrgen;
    private Boolean isManual;
    private Boolean isExpedition;
}
