package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Pricelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PricelistRepository extends JpaRepository<Pricelist, Long> {
    Optional<Pricelist> findByItemName(String itemName);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE pricelist RESTART IDENTITY", nativeQuery = true)
    void truncateTable();
}
