package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.StatusServis;
import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaksiServisRepository extends JpaRepository<TransaksiServis, UUID>, JpaSpecificationExecutor<TransaksiServis> {
    // Untuk pencarian via nomor nota/resi
    Optional<TransaksiServis> findByNoServis(String noServis);

    // Di TransaksiServisRepository.java
    Optional<TransaksiServis> findById(UUID id);
    
    // Untuk pencarian via Link otomatis / QR Code tanpa login
    Optional<TransaksiServis> findByTrackingToken(UUID trackingToken);

    // Tambahkan ini di dalam interface TransaksiServisRepository
    @Query("SELECT t FROM TransaksiServis t WHERE t.pelanggan.noTelepon = :noTelepon ORDER BY t.createdAt DESC")
    List<TransaksiServis> findByPelangganNoTelepon(@Param("noTelepon") String noTelepon);

    List<TransaksiServis> findByStatusTerkiniAndCreatedAtBetween(StatusServis status, LocalDateTime start, LocalDateTime end);

    List<TransaksiServis> findByStatusTerkiniOrderByCreatedAtDesc(StatusServis status);

    List<TransaksiServis> findByTglBatasGaransiAfter(LocalDateTime sekarang);

    @Query("SELECT t FROM TransaksiServis t WHERE t.statusTerkini = 'SUDAH_DIAMBIL' AND t.tglBatasGaransi IS NOT NULL AND t.tglBatasGaransi > :sekarang ORDER BY t.tglBatasGaransi ASC")
    List<TransaksiServis> findAktifGaransi(@Param("sekarang") LocalDateTime sekarang);

    @Query("SELECT t FROM TransaksiServis t WHERE t.statusTerkini = 'SUDAH_DIAMBIL' AND t.tglBatasGaransi IS NOT NULL AND t.tglBatasGaransi <= :sekarang ORDER BY t.tglBatasGaransi DESC")
    List<TransaksiServis> findExpiredGaransi(@Param("sekarang") LocalDateTime sekarang);

    // Untuk riwayat servis per pelanggan (user)
    @Query("SELECT t FROM TransaksiServis t WHERE t.pelanggan.id = :pelangganId ORDER BY t.createdAt DESC")
    List<TransaksiServis> findByPelangganIdOrderByCreatedAtDesc(@Param("pelangganId") UUID pelangganId);
}
