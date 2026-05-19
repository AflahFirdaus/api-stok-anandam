package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.PelangganMybiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PelangganMybizRepository extends JpaRepository<PelangganMybiz, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE pelanggan_mybiz RESTART IDENTITY", nativeQuery = true)
    void truncateTable();

    java.util.List<PelangganMybiz> findByNamaPartnerContainingIgnoreCase(String query);
}

