package com.stok.anandam.store.core.postgres.repository;

import com.stok.anandam.store.core.postgres.model.Tkdn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TkdnRepository extends JpaRepository<Tkdn, Integer> {

        @Modifying
        @Transactional
        @Query(value = "TRUNCATE TABLE tkdn RESTART IDENTITY", nativeQuery = true)
        void truncateTable();

        // Fitur: Cari berdasarkan Nama + Pagination otomatis
        Page<Tkdn> findByNamaContainingIgnoreCase(String nama, Pageable pageable);

        // Fitur: Filter berdasarkan Kategori + Pagination
        Page<Tkdn> findByKategoriIgnoreCase(String kategori, Pageable pageable);

        @Query(value = "SELECT t FROM Tkdn t WHERE " +
                        "(:isTkdn IS NULL OR " +
                        "  (:isTkdn = true AND t.sertifikatTkd IS NOT NULL AND t.sertifikatTkd <> '') OR " +
                        "  (:isTkdn = false AND (t.sertifikatTkd IS NULL OR t.sertifikatTkd = ''))) " +
                        "AND (:kategori IS NULL OR :kategori = '' OR LOWER(t.kategori) = LOWER(:kategori)) " +
                        "AND (:search IS NULL OR :search = '' OR " +
                        "  LOWER(t.nama) LIKE LOWER(:search) OR " +
                        "  LOWER(t.noMerek) LIKE LOWER(:search) OR " +
                        "  LOWER(t.kategori) LIKE LOWER(:search))", countQuery = "SELECT COUNT(t) FROM Tkdn t WHERE " +
                                        "(:isTkdn IS NULL OR " +
                                        "  (:isTkdn = true AND t.sertifikatTkd IS NOT NULL AND t.sertifikatTkd <> '') OR "
                                        +
                                        "  (:isTkdn = false AND (t.sertifikatTkd IS NULL OR t.sertifikatTkd = ''))) " +
                                        "AND (:kategori IS NULL OR :kategori = '' OR LOWER(t.kategori) = LOWER(:kategori)) "
                                        +
                                        "AND (:search IS NULL OR :search = '' OR " +
                                        "  LOWER(t.nama) LIKE LOWER(:search) OR " +
                                        "  LOWER(t.noMerek) LIKE LOWER(:search) OR " +
                                        "  LOWER(t.kategori) LIKE LOWER(:search))")
        Page<Tkdn> findByFilters(
                        @Param("isTkdn") Boolean isTkdn,
                        @Param("kategori") String kategori,
                        @Param("search") String search,
                        Pageable pageable);

        @Query(value = "SELECT t FROM Tkdn t WHERE " +
                        "(:isTkdn IS NULL OR " +
                        "  (:isTkdn = true AND t.sertifikatTkd IS NOT NULL AND t.sertifikatTkd <> '') OR " +
                        "  (:isTkdn = false AND (t.sertifikatTkd IS NULL OR t.sertifikatTkd = ''))) " +
                        "AND (:kategori IS NULL OR :kategori = '' OR LOWER(t.kategori) = LOWER(:kategori)) " +
                        "AND (:search IS NULL OR :search = '' OR " +
                        "  LOWER(t.nama) LIKE LOWER(:search) OR " +
                        "  LOWER(t.noMerek) LIKE LOWER(:search) OR " +
                        "  LOWER(t.kategori) LIKE LOWER(:search)) " +
                        "AND (:processor IS NULL OR :processor = '' OR LOWER(t.processor) LIKE LOWER(:processor)) " +
                        "AND (:ram IS NULL OR :ram = '' OR LOWER(t.ram) LIKE LOWER(:ram)) " +
                        "AND (:ssd IS NULL OR :ssd = '' OR LOWER(t.ssd) LIKE LOWER(:ssd)) " +
                        "AND (:hdd IS NULL OR :hdd = '' OR LOWER(t.hdd) LIKE LOWER(:hdd)) " +
                        "AND (:vga IS NULL OR :vga = '' OR LOWER(t.vga) LIKE LOWER(:vga)) " +
                        "AND (:layar IS NULL OR :layar = '' OR LOWER(t.layar) LIKE LOWER(:layar)) " +
                        "AND (:os IS NULL OR :os = '' OR LOWER(t.os) LIKE LOWER(:os))", countQuery = "SELECT COUNT(t) FROM Tkdn t WHERE "
                                        +
                                        "(:isTkdn IS NULL OR " +
                                        "  (:isTkdn = true AND t.sertifikatTkd IS NOT NULL AND t.sertifikatTkd <> '') OR "
                                        +
                                        "  (:isTkdn = false AND (t.sertifikatTkd IS NULL OR t.sertifikatTkd = ''))) " +
                                        "AND (:kategori IS NULL OR :kategori = '' OR LOWER(t.kategori) = LOWER(:kategori)) "
                                        +
                                        "AND (:search IS NULL OR :search = '' OR " +
                                        "  LOWER(t.nama) LIKE LOWER(:search) OR " +
                                        "  LOWER(t.noMerek) LIKE LOWER(:search) OR " +
                                        "  LOWER(t.kategori) LIKE LOWER(:search)) " +
                                        "AND (:processor IS NULL OR :processor = '' OR LOWER(t.processor) LIKE LOWER(:processor)) "
                                        +
                                        "AND (:ram IS NULL OR :ram = '' OR LOWER(t.ram) LIKE LOWER(:ram)) " +
                                        "AND (:ssd IS NULL OR :ssd = '' OR LOWER(t.ssd) LIKE LOWER(:ssd)) " +
                                        "AND (:hdd IS NULL OR :hdd = '' OR LOWER(t.hdd) LIKE LOWER(:hdd)) " +
                                        "AND (:vga IS NULL OR :vga = '' OR LOWER(t.vga) LIKE LOWER(:vga)) " +
                                        "AND (:layar IS NULL OR :layar = '' OR LOWER(t.layar) LIKE LOWER(:layar)) " +
                                        "AND (:os IS NULL OR :os = '' OR LOWER(t.os) LIKE LOWER(:os))")
        Page<Tkdn> findByFiltersWithSpec(
                        @Param("isTkdn") Boolean isTkdn,
                        @Param("kategori") String kategori,
                        @Param("search") String search,
                        @Param("processor") String processor,
                        @Param("ram") String ram,
                        @Param("ssd") String ssd,
                        @Param("hdd") String hdd,
                        @Param("vga") String vga,
                        @Param("layar") String layar,
                        @Param("os") String os,
                        Pageable pageable);

        @Query("SELECT DISTINCT t.kategori FROM Tkdn t WHERE t.kategori IS NOT NULL AND TRIM(t.kategori) <> '' ORDER BY t.kategori")
        java.util.List<String> findDistinctKategoriOrderByKategori();
}