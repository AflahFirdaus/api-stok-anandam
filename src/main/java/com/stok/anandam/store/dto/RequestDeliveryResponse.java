package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDeliveryResponse {
    private Long id;
    private String nomorRequest;
    private String receiverName;
    private String receiverPhone;
    private String alamatLengkap;
    private String alamatMaps;
    private String keterangan;
    private RequestDeliveryStatus status;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long penjadwalanId;
    private Boolean isUrgen;
}
