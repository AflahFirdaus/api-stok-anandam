package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoRepository extends JpaRepository<Memo, UUID> {
    
    Optional<Memo> findByNomorMemo(String nomorMemo);
    
    List<Memo> findByStatusAkhirOrderByTanggalMemoDesc(String statusAkhir);
    
    List<Memo> findByMarketingId(Long marketingId);
    
    @Query("SELECT m FROM Memo m WHERE m.isDeliveryRequired = true AND m.statusAkhir = :status")
    List<Memo> findUnscheduledMemos(@Param("status") String status);

    // Ambil 1 data terbaru berdasarkan waktu pembuatan
    Optional<Memo> findTopByOrderByCreatedAtDesc();

    java.util.List<com.stok.anandam.store.core.postgres.model.Memo> findAllByOrderByCreatedAtDesc();

    // Untuk Dashboard Gudang / Marketing melihat list berdasarkan status
    java.util.List<com.stok.anandam.store.core.postgres.model.Memo> findByStatusAkhirOrderByCreatedAtDesc(com.stok.anandam.store.core.postgres.model.enums.MemoStatus statusAkhir);

    boolean existsByOrderIdMarketplaceAndStatusAkhirNot(String orderIdMarketplace, com.stok.anandam.store.core.postgres.model.enums.MemoStatus statusAkhir);

    boolean existsByOrderIdMarketplaceAndStatusAkhirNotAndIdNot(String orderIdMarketplace, com.stok.anandam.store.core.postgres.model.enums.MemoStatus statusAkhir, UUID id);

    boolean existsByOrderIdMarketplace(String orderIdMarketplace);

    boolean existsByOrderIdMarketplaceAndIdNot(String orderIdMarketplace, UUID id);

    @Query("SELECT m.statusAkhir, COUNT(m.id) FROM Memo m GROUP BY m.statusAkhir")
    List<Object[]> countAllStatuses();

    @Query("SELECT m.statusAkhir, COUNT(m.id) FROM Memo m WHERE " +
           "m.marketingEmpCode = :empCode OR " +
           "m.creator.id = :userId OR " +
           "m.creator.role = :role OR " +
           "m.id IN :assignedMemoIds " +
           "GROUP BY m.statusAkhir")
    List<Object[]> countStatusesForMarketingWithAssigned(
            @Param("empCode") String empCode, 
            @Param("userId") Long userId, 
            @Param("role") com.stok.anandam.store.core.postgres.model.Role role,
            @Param("assignedMemoIds") List<UUID> assignedMemoIds);

    @Query("SELECT m.statusAkhir, COUNT(m.id) FROM Memo m WHERE " +
           "m.marketingEmpCode = :empCode OR " +
           "m.creator.id = :userId OR " +
           "m.creator.role = :role " +
           "GROUP BY m.statusAkhir")
    List<Object[]> countStatusesForMarketingWithoutAssigned(
            @Param("empCode") String empCode, 
            @Param("userId") Long userId, 
            @Param("role") com.stok.anandam.store.core.postgres.model.Role role);

    @Query("SELECT m.statusAkhir, COUNT(m.id) FROM Memo m WHERE m.id IN :assignedMemoIds GROUP BY m.statusAkhir")
    List<Object[]> countStatusesForDelivery(@Param("assignedMemoIds") List<UUID> assignedMemoIds);

    @Query("SELECT m.statusAkhir, COUNT(m.id) FROM Memo m WHERE m.statusAkhir IN :statuses GROUP BY m.statusAkhir")
    List<Object[]> countStatusesByStatusList(@Param("statuses") List<com.stok.anandam.store.core.postgres.model.enums.MemoStatus> statuses);

    // Untuk scheduler auto-match JL: cari memo dengan status tertentu yang belum punya nomor JL
    @Query("SELECT m FROM Memo m WHERE m.statusAkhir IN :statuses AND (m.nomorJl IS NULL OR m.nomorJl = '')")
    List<com.stok.anandam.store.core.postgres.model.Memo> findByStatusAkhirInAndNomorJlIsNullOrEmpty(
            @Param("statuses") List<com.stok.anandam.store.core.postgres.model.enums.MemoStatus> statuses);

    // Search memo by resi (case-insensitive partial match)
    List<Memo> findByResiIgnoreCaseContaining(String resi);
    
    // Search memo by orderIdMarketplace (case-insensitive partial match)
    List<Memo> findByOrderIdMarketplaceIgnoreCaseContaining(String orderId);

    // Search exact match by resi (prioritas utama untuk scan barcode)
    List<Memo> findByResiIgnoreCase(String resi);

    // Search exact match by orderIdMarketplace (prioritas kedua)
    List<Memo> findByOrderIdMarketplaceIgnoreCase(String orderId);
}
