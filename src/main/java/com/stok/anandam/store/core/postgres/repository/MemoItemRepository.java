package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.MemoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoItemRepository extends JpaRepository<MemoItem, Long> {

    List<MemoItem> findByMemoIdAndDeletedAtIsNull(UUID memoId);
    
    List<MemoItem> findByMemoId(UUID memoId);

    List<MemoItem> findByMemo_StatusAkhir(com.stok.anandam.store.core.postgres.model.enums.MemoStatus status);

    void deleteByMemoId(UUID memoId);
}