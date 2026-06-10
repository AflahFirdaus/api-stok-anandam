package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // Spring Data JPA akan otomatis meng-generate query berdasarkan nama method ini
    List<AuditLog> findByEntityIdOrderByChangedAtDesc(String entityId);
}