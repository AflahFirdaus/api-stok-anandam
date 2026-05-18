package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.MemoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoItemRepository extends JpaRepository<MemoItem, Long> {

    List<MemoItem> findByMemoIdAndDeletedAtIsNull(UUID memoId);
    
    List<MemoItem> findByMemoId(UUID memoId);

    List<MemoItem> findByMemoIdIn(List<UUID> memoIds);

    List<MemoItem> findByMemo_StatusAkhir(com.stok.anandam.store.core.postgres.model.enums.MemoStatus status);

    @Query("SELECT mi FROM MemoItem mi " +
           "LEFT JOIN FETCH mi.memo m " +
           "LEFT JOIN FETCH m.marketing mkt " +
           "WHERE m.statusAkhir = :status " +
           "AND mi.deletedAt IS NULL AND m.deletedAt IS NULL")
    List<MemoItem> findByMemo_StatusAkhirWithMemoAndMarketing(@Param("status") com.stok.anandam.store.core.postgres.model.enums.MemoStatus status);

    @Query("SELECT mi FROM MemoItem mi JOIN mi.memo m WHERE m.statusAkhir NOT IN :terminalStatuses AND mi.deletedAt IS NULL AND m.deletedAt IS NULL")
    List<MemoItem> findPendingItemsExcluding(@Param("terminalStatuses") List<com.stok.anandam.store.core.postgres.model.enums.MemoStatus> terminalStatuses);

    @Query("SELECT mi FROM MemoItem mi JOIN mi.memo m WHERE m.statusAkhir IN :statuses AND mi.deletedAt IS NULL AND m.deletedAt IS NULL")
    List<MemoItem> findPendingItemsByStatuses(@Param("statuses") List<com.stok.anandam.store.core.postgres.model.enums.MemoStatus> statuses);

    public interface PendingSummary {
        Long getTotalQty();
        java.math.BigDecimal getTotalValue();
    }

    @Query(value = "SELECT " +
           "CAST(SUM(mi.qty - COALESCE(mi.qty_shipped, 0)) AS BIGINT) as \"totalQty\", " +
           "CAST(SUM((mi.qty - COALESCE(mi.qty_shipped, 0)) * COALESCE(s.harga_hpp, 0)) AS NUMERIC) as \"totalValue\" " +
           "FROM memo_items mi " +
           "JOIN memos m ON mi.memo_id = m.id " +
           "LEFT JOIN stok s ON LOWER(TRIM(mi.nama_barang)) = LOWER(TRIM(s.item_name)) " +
           "WHERE m.status_akhir = 'DISETUJUI' " +
           "AND mi.deleted_at IS NULL AND m.deleted_at IS NULL", nativeQuery = true)
    PendingSummary calculatePendingSummaryNative();

    void deleteByMemoId(UUID memoId);
}