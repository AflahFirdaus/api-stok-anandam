package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    // Biarkan kosong.
    // Spring Boot otomatis membuatkan codingan untuk method:
    // .save(), .findById(), .findAll(), .delete(), dll.
}