package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.KlaimDistributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KlaimDistributorRepository extends JpaRepository<KlaimDistributor, UUID> {
    java.util.Optional<KlaimDistributor> findByTransaksiId(java.util.UUID transaksiId);
}
