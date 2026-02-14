package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Stock;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE stok RESTART IDENTITY", nativeQuery = true)
    void truncateTable();

    // Cari berdasarkan Item Code (Exact match atau Partial?)
    // Kita pakai partial biar gampang:
    // Cari berdasarkan Nama Item (Case Insensitive)
    Page<Stock> findByItemNameContainingIgnoreCase(String itemName, Pageable pageable);

    // Cari berdasarkan Item Code (Case Insensitive)
    Page<Stock> findByItemCodeContainingIgnoreCase(String itemCode, Pageable pageable);

    // Cari Global (Bisa ketik Kode atau Nama di satu kolom search)
    Page<Stock> findByItemNameContainingIgnoreCaseOrItemCodeContainingIgnoreCase(String itemName, String itemCode, Pageable pageable);

    long countByFinalStokLessThan(Integer threshold);
    List<Stock> findTop5ByFinalStokLessThanOrderByFinalStokAsc(Integer threshold);
}