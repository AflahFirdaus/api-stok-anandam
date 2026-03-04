package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.OldItemSerialNumber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OldItemSnRepository extends JpaRepository<OldItemSerialNumber, Long> {

        @Query("SELECT sn FROM OldItemSerialNumber sn WHERE " +
                        "(sn.tanggal BETWEEN :startDate AND :endDate) AND " +
                        "(:type IS NULL OR :type = '' OR sn.type = :type) AND " +
                        "(:search IS NULL OR :search = '' OR " +
                        "LOWER(sn.docId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(sn.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(sn.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(sn.sn) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<OldItemSerialNumber> findByFilters(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("type") String type,
                        @Param("search") String search,
                        Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(sn.tanggal) FROM OldItemSerialNumber sn")
        java.util.Optional<LocalDateTime> findMaxTanggal();
}
