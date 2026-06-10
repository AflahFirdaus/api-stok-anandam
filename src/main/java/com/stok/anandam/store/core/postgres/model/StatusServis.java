package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

public enum StatusServis {
    BELUM_CEK("Belum Cek"),
    SEDANG_CEK("Sedang Cek"),
    SEDANG_DIKERJAKAN("Sedang Dikerjakan"),
    SEDANG_TES("Sedang Tes"),
    TUNGGU_KONFIRMASI("Tunggu Konfirmasi"),
    TUNGGU_SPAREPART("Tunggu Sparepart"),
    BISA_DIAMBIL("Bisa Diambil"),
    SUDAH_DIAMBIL("Sudah Diambil"),
    BATAL("Batal"),
    
    // --- STATUS INTERNAL DISTRIBUTOR / KLAIM ---
    KLAIM_MENUNGGU_PENGIRIMAN("Klaim: Menunggu Pengiriman"),
    KLAIM_DIKIRIM("Klaim: Sedang Dikirim"),
    KLAIM_SUDAH_DIKIRIM("Klaim: Sudah Sampai Distributor"),
    KLAIM_SUDAH_DIAMBIL("Klaim: Sudah Diambil dari Distributor");

    private final String value;

    StatusServis(String value) { 
        this.value = value; 
    }

    public String getValue() { 
        return value; 
    }

    @Converter(autoApply = true)
    public static class MappedConverter implements AttributeConverter<StatusServis, String> {
        @Override
        public String convertToDatabaseColumn(StatusServis attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public StatusServis convertToEntityAttribute(String dbData) {
            if (dbData == null) return null;
            return Stream.of(StatusServis.values())
                    .filter(c -> c.getValue().equals(dbData))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown database value: " + dbData));
        }
    }
}