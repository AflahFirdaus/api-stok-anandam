package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoDetailResponse {
    private UUID id;
    private String nomorMemo;
    private Long customerId;
    private String customerName; // Added for convenience
    private String customerPhone;
    private Long marketingId;
    private String marketingName;
    private String marketingEmpCode;
    private String marketingUsername;
    private String creatorName;
    private LocalDateTime tanggalMemo;
    private Boolean isTeknisRequired;
    private Boolean isDeliveryRequired;
    private String opsiPengiriman;
    private String metodePembayaran;
    private String qrCode;

    private String memoType;
    private String orderIdMarketplace;
    private String resi;
    private String ekspedisi;
    private String platform;
    private String kodePos; // Added for display in list
    private String buktiFoto;
    private String buktiFotoUrl;

    private MemoStatus statusAkhir;
    private BigDecimal totalHarga;
    private String deskripsi;
    private String nomorJl;
    private String tempo;
    private String badanUsaha;
    
    // Geographic fallbacks (from postal code table)
    private String desaKelurahan;
    private String kecamatan;
    private String kabupatenKota;

    private List<MemoItemResponse> items; 
    private List<MemoLogResponse> logs;   
    private List<PenjadwalanResponse> penjadwalanHistory;
}