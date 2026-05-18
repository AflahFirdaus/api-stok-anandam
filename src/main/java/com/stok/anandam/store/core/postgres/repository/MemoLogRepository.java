package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.MemoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemoLogRepository extends JpaRepository<MemoLog, Long> {

    // Mengambil riwayat log urut dari yang terbaru
    java.util.List<com.stok.anandam.store.core.postgres.model.MemoLog> findByMemoIdOrderByCreatedAtDesc(java.util.UUID memoId);

    java.util.List<com.stok.anandam.store.core.postgres.model.MemoLog> findByMemoIdInOrderByCreatedAtDesc(java.util.List<java.util.UUID> memoIds);
    
}