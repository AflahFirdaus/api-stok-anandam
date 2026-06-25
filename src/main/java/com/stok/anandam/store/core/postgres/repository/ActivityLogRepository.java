package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    // Filter by username (case-insensitive, partial match)
    Page<ActivityLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    // Filter by action (case-insensitive, partial match)
    Page<ActivityLog> findByActionContainingIgnoreCase(String action, Pageable pageable);

    // Filter kombinasi: username DAN action (case-insensitive, partial match)
    Page<ActivityLog> findByUsernameContainingIgnoreCaseAndActionContainingIgnoreCase(
            String username, String action, Pageable pageable);

    // Ambil log terakhir berdasarkan awalan action
    java.util.Optional<ActivityLog> findFirstByActionStartingWithOrderByTimestampDesc(String actionPrefix);

    // Hitung jumlah username unik yang aktif hari ini
    @Query("SELECT COUNT(DISTINCT a.username) FROM ActivityLog a WHERE CAST(a.timestamp AS date) = CURRENT_DATE")
    long countDistinctActiveUsersToday();

    // Daftar username unik yang aktif hari ini
    @Query("SELECT DISTINCT a.username FROM ActivityLog a WHERE CAST(a.timestamp AS date) = CURRENT_DATE ORDER BY a.username")
    List<String> findDistinctActiveUsernamesToday();

    // Statistik harian: jumlah user unik per hari dalam rentang tanggal tertentu
    @Query("SELECT CAST(a.timestamp AS date) as logDate, COUNT(DISTINCT a.username) as userCount " +
           "FROM ActivityLog a " +
           "WHERE CAST(a.timestamp AS date) >= :startDate " +
           "GROUP BY CAST(a.timestamp AS date) " +
           "ORDER BY CAST(a.timestamp AS date) ASC")
    List<Object[]> findDailyActiveUserStats(@Param("startDate") LocalDate startDate);

}