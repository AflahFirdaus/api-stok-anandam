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
}