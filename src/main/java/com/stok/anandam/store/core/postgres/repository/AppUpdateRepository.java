package com.stok.anandam.store.core.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stok.anandam.store.core.postgres.model.AppUpdate;

import java.util.Optional;

@Repository
public interface AppUpdateRepository extends JpaRepository<AppUpdate, Long> {
    // Mengambil versi dengan versionCode paling tinggi
    Optional<AppUpdate> findFirstByOrderByVersionCodeDesc();
}

