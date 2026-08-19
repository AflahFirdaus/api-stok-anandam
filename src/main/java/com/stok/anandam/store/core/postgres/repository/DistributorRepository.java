package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Distributor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE distributor RESTART IDENTITY", nativeQuery = true)
    void truncateTable();
}