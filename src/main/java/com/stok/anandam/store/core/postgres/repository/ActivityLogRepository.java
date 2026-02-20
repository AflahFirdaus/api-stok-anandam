package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    // Filter by username (case-insensitive, partial match)
    Page<ActivityLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    
    // Filter by action (case-insensitive, partial match)
    Page<ActivityLog> findByActionContainingIgnoreCase(String action, Pageable pageable);
    
    // Filter kombinasi: username DAN action (case-insensitive, partial match)
    Page<ActivityLog> findByUsernameContainingIgnoreCaseAndActionContainingIgnoreCase(
            String username, String action, Pageable pageable);
}