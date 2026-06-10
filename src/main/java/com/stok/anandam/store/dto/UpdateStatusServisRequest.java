package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.StatusPembayaran;
import com.stok.anandam.store.core.postgres.model.StatusServis;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateStatusServisRequest {
    private StatusServis statusBaru;
    private String kondisiServis;
    private String ketTindakan; 
    private Long teknisiId; 
    private Long penyerahId;   
    private String pengambilNama;    
    private String durasiGaransi;     
    private LocalDate tglDitangani;
    private BigDecimal estimasiBiaya;
    private BigDecimal biayaFinal;
    private BigDecimal modalSparepart;
    private StatusPembayaran statusBayar;
    private LocalDate tglJatuhTempo;
    private LocalDate tglAmbil;
    private String catatanPublikLog;
    private String modelSeriBaru;
}