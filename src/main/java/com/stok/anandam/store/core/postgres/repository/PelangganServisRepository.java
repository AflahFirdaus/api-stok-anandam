package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.PelangganServis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PelangganServisRepository extends JpaRepository<PelangganServis, UUID> {
    
    // Pencarian berdasarkan Nama (mengabaikan huruf besar/kecil) atau Nomor Telepon
    @Query("SELECT p FROM PelangganServis p WHERE " +
           "LOWER(p.namaPelanggan) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "p.noTelepon LIKE CONCAT('%', :search, '%')")
    Page<PelangganServis> searchByNamaOrTelepon(@Param("search") String search, Pageable pageable);

    boolean existsByNoTelepon(String noTelepon);
}