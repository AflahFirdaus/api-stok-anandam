package com.stok.anandam.store.core.postgres.model.enums;

public enum MemoStatus {
    DRAFT,
    MENUNGGU_PERSETUJUAN,
    DISETUJUI,
    DITOLAK,
    MENUNGGU_GUDANG,
    MENUNGGU_TEKNISI,
    PROSES_TEKNISI,
    BUFFER_ZONE,
    MENUNGGU_PENGIRIMAN,
    DALAM_PENGIRIMAN,
    DITERIMA_USER,
    TERKIRIM_SEBAGIAN,
    KENDALA_BARANG,
    MENUNGGU_NOTA,
    DIBUAT_NOTA,
    MENUNGGU_EXPEDISI,
    MENUNGGU_KONFIRMASI_PICKUP,
    SELESAI,
    DIBATALKAN, DIJADWALKAN, PENDING, DELETED;

    public static MemoStatus fromName(String name) {
        if (name == null) return null;
        return switch (name.toUpperCase()) {
            case "PROSES_GUDANG", "DITERIMA_GUDANG" -> MENUNGGU_GUDANG;
            case "MENUNGGU_NOTA" -> MENUNGGU_NOTA;
            case "SIAP_PENUGASAN", "SIAP_DIAMBIL" -> MENUNGGU_PENGIRIMAN;
            case "TEKNISI_SELESAI" -> BUFFER_ZONE;
            case "SUDAH_DIKIRIM", "DITERIMA_USER" -> DITERIMA_USER;
            default -> {
                try {
                    yield valueOf(name.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Default fallback for unknown status
                    yield DRAFT;
                }
            }
        };
    }
}