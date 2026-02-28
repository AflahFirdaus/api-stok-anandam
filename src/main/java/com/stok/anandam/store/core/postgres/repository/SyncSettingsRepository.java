package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.SyncSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncSettingsRepository extends JpaRepository<SyncSettings, String> {
    Optional<SyncSettings> findBySyncKey(String syncKey);
}
