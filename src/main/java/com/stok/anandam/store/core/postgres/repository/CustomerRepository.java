package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Cari pelanggan yang aktif (tidak di-soft delete)
    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL")
    List<Customer> findAllActive();

    // Cari berdasarkan nama (untuk fitur search di Flutter)
    List<Customer> findByNamaPelangganContainingIgnoreCaseAndDeletedAtIsNull(String nama);

    // Cari berdasarkan nomor HP untuk validasi data ganda
    Optional<Customer> findByNoHpAndDeletedAtIsNull(String noHp);

    // Tambahkan baris ini di dalam interface-nya
    Optional<Customer> findByNoHp(String noHp);

    
}