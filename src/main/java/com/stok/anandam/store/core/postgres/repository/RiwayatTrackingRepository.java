package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.RiwayatTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface RiwayatTrackingRepository extends JpaRepository<RiwayatTracking, UUID> {
    // Mengambil histori urut dari waktu terbaru
    List<RiwayatTracking> findByTransaksiIdOrderByWaktuUpdateDesc(UUID transaksiId);
}