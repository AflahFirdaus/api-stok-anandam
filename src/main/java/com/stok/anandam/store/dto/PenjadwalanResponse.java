package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.enums.StatusJadwal;
import com.stok.anandam.store.core.postgres.model.enums.TipeTugas;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PenjadwalanResponse {
    private Long id;
    private UUID memoId;
    private String nomorMemo;
    private Long requestDeliveryId;
    private String nomorRequest;
    private TipeTugas tipeTugas;
    private StatusJadwal statusJadwal;
    private String alamatLengkap;
    private String alamatMaps;
    private Integer idKodepos;
    private String estimasiWaktu;
    private String catatan;
    private String namaPenerima;
    private String fotoBukti;
    private String catatanOperasional;
    private String tanggalJadwal;
    private Long personelId;
    private String personelName;
    private String personelRole;
    private String kodePos;
    private Boolean isUrgen;
    private String marketingName;
    private String manualCustomerName;
    private String manualNoHp;
    private Boolean isExpeditionOutlet;
    private List<String> manifestNomors;

    
    // Alamat components for filtering
    private String kecamatan;
    private String desaKelurahan;
    private String kabupatenKota;

    private Double latitude;
    private Double longitude;
}