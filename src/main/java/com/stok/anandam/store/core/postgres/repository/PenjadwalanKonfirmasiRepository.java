package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.PenjadwalanKonfirmasi;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PenjadwalanKonfirmasiRepository extends JpaRepository<PenjadwalanKonfirmasi, Long> {

    // Eager-load semua asosiasi dalam 1 JOIN query untuk menghilangkan N+1
    @EntityGraph(attributePaths = {"memo", "requestDelivery", "kodepos", "manifestMemos", "manifestRequests"})
    List<PenjadwalanKonfirmasi> findAllByOrderByCreatedAtDesc();

    List<PenjadwalanKonfirmasi> findByMemo_Id(UUID memoId);
    
    List<PenjadwalanKonfirmasi> findByMemo_IdAndDeletedAtIsNull(UUID memoId);

    List<PenjadwalanKonfirmasi> findByMemo_IdInAndDeletedAtIsNull(List<UUID> memoIds);

    // Cari jadwal spesifik tipe tertentu
    Optional<PenjadwalanKonfirmasi> findByMemo_IdAndTipeTugasAndDeletedAtIsNull(UUID memoId, com.stok.anandam.store.core.postgres.model.enums.TipeTugas tipeTugas);

    // List tugas untuk Driver/Teknisi tertentu yang belum selesai
    List<PenjadwalanKonfirmasi> findByPersonelIdAndStatusJadwalAndDeletedAtIsNull(Long personelId, String statusJadwal);

    // Untuk Dashboard Gudang: Lihat semua yang berstatus 'MENUNGGU KONFIRMASI'
    // Eager-load asosiasi untuk menghilangkan N+1 pada getListTugas
    @EntityGraph(attributePaths = {"memo", "requestDelivery", "kodepos", "manifestMemos", "manifestRequests"})
    List<PenjadwalanKonfirmasi> findByStatusJadwalAndDeletedAtIsNullOrderByCreatedAtDesc(com.stok.anandam.store.core.postgres.model.enums.StatusJadwal statusJadwal);

    // Eager-load asosiasi untuk menghilangkan N+1 pada getListTugas (dengan filter tipe+status)
    @EntityGraph(attributePaths = {"memo", "requestDelivery", "kodepos", "manifestMemos", "manifestRequests"})
    java.util.List<PenjadwalanKonfirmasi> findByTipeTugasAndStatusJadwalOrderByCreatedAtDesc(
            com.stok.anandam.store.core.postgres.model.enums.TipeTugas tipeTugas,
            com.stok.anandam.store.core.postgres.model.enums.StatusJadwal statusJadwal
    );

    List<PenjadwalanKonfirmasi> findByMemoIsNullAndDeletedAtIsNull();

    List<PenjadwalanKonfirmasi> findByPersonelIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long personelId);

    List<PenjadwalanKonfirmasi> findByPersonelIdAndDeletedAtIsNull(Long personelId);
}