package com.stok.anandam.store.service;

import com.stok.anandam.store.dto.CanvasVisitRecord;
import com.stok.anandam.store.dto.PelangganItem;
import com.stok.anandam.store.dto.PelangganOption;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Akses fitur Canvas / Pelanggan langsung ke database Portal (eksternal),
 * untuk mendukung fitur "Catat Canvas" yang dipindah dari DB lokal.
 */
@Service
public class CanvasingPortalService {

    private static final Logger log = LoggerFactory.getLogger(CanvasingPortalService.class);

    private static final String[] MONTHS = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    @Autowired
    @Qualifier("portalJdbcTemplate")
    private JdbcTemplate portalJdbcTemplate;

    // ============ DAFTAR PELANGGAN (dropbox) ============
    public List<PelangganOption> getPelangganOptions(String search, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.\"namaInstansi\" FROM \"Pelanggan\" p WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND LOWER(p.\"namaInstansi\") LIKE LOWER(?)");
            args.add("%" + search.trim() + "%");
        }
        sql.append(" ORDER BY p.\"namaInstansi\" ASC LIMIT ?");
        args.add(Math.min(limit, 100));

        return portalJdbcTemplate.query(sql.toString(), args.toArray(),
                (rs, i) -> new PelangganOption(rs.getString("id"), rs.getString("namaInstansi")));
    }

    // ============ DAFTAR PELANGGAN (paginasi) ============
    public Page<PelangganItem> getAllPelanggan(int page, int size, String sortBy, String direction,
                                               String search, String kategori, String provinsi) {
        String orderCol = switch (sortBy == null || sortBy.isBlank() ? "namaInstansi" : sortBy) {
            case "kategori" -> "\"kategori\"";
            case "provinsi" -> "\"provinsi\"";
            default -> "\"namaInstansi\"";
        };
        boolean asc = direction == null || !direction.equalsIgnoreCase("desc");

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> whereArgs = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            where.append(" AND LOWER(p.\"namaInstansi\") LIKE LOWER(?)");
            whereArgs.add("%" + search.trim() + "%");
        }
        if (kategori != null && !kategori.isBlank()) {
            where.append(" AND p.kategori = ?");
            whereArgs.add(kategori);
        }
        if (provinsi != null && !provinsi.isBlank()) {
            where.append(" AND p.provinsi = ?");
            whereArgs.add(provinsi);
        }

        Integer total = portalJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"Pelanggan\" p" + where,
                Integer.class, whereArgs.toArray());
        int totalInt = total == null ? 0 : total;

        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.\"namaInstansi\", p.kategori, p.kabupaten, p.provinsi, p.alamat "
                        + "FROM \"Pelanggan\" p")
                .append(where)
                .append(" ORDER BY ").append(orderCol).append(asc ? " ASC" : " DESC")
                .append(" LIMIT ? OFFSET ?");
        List<Object> listArgs = new ArrayList<>(whereArgs);
        listArgs.add(size);
        listArgs.add((long) page * size);

        List<PelangganItem> list = portalJdbcTemplate.query(sql.toString(), listArgs.toArray(),
                (rs, i) -> PelangganItem.builder()
                        .id(rs.getString("id"))
                        .namaInstansi(rs.getString("namaInstansi"))
                        .kategori(rs.getString("kategori"))
                        .kabupaten(rs.getString("kabupaten"))
                        .provinsi(rs.getString("provinsi"))
                        .alamat(rs.getString("alamat"))
                        .build());

        Pageable pageable = PageRequest.of(page, size);
        if (totalInt == 0) return new PageImpl<>(list, pageable, 0);
        if (page >= (int) Math.ceil(totalInt / (double) size)) {
            return getAllPelanggan(0, size, sortBy, direction, search, kategori, provinsi);
        }
        return new PageImpl<>(list, pageable, totalInt);
    }
// ============ CATAT KUNJUNGAN (INSERT ke Canvas) ============
    public String createCanvas(String pelangganId, LocalDate tanggal,
                               String kunjungan, String keterangan, String catatan) {
        // 1. Validasi pelanggan ada di DB Portal
        Integer exists = portalJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"Pelanggan\" WHERE id = ?", Integer.class, pelangganId);
        if (exists == null || exists == 0) {
            throw new ResourceNotFoundException("Pelanggan dengan ID " + pelangganId + " tidak ditemukan");
        }

        LocalDate tgl = (tanggal != null) ? tanggal : LocalDate.now();
        String k = normalizeKunjungan(kunjungan);
        String id = UUID.randomUUID().toString();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // 2. Null-safe: empty string → null (agar tidak konflik constraint DB)
        String ket = (keterangan != null && !keterangan.isBlank()) ? keterangan.trim() : null;
        String cat = (catatan != null && !catatan.isBlank()) ? catatan.trim() : null;

        // 3. Gunakan java.sql.Date.valueOf(tgl) — kompatibel dengan kolom DATE maupun TIMESTAMPTZ di PostgreSQL
        try {
            portalJdbcTemplate.update(
                    "INSERT INTO \"Canvas\" (id, \"pelangganId\", tanggal, kunjungan, keterangan, catatan, \"createdAt\", \"updatedAt\") "
                            + "VALUES (?, ?, ?, ?::\"KunjunganCanvas\", ?, ?, ?, ?)",
                    id, pelangganId, Date.valueOf(tgl), k, ket, cat, now, now);
        } catch (Exception e) {
            log.error("Gagal INSERT ke tabel Canvas. pelangganId={}, tanggal={}, kunjungan={}, error={}",
                    pelangganId, tgl, k, e.getMessage(), e);
            throw e;  // re-throw agar GlobalExceptionHandler tetap menangani
        }
        return id;
    }

    private String normalizeKunjungan(String kunjungan) {
        if (kunjungan != null && kunjungan.trim().equalsIgnoreCase("VISIT")) return "VISIT";
        return "CANVAS";
    }
// ============ RIWAYAT KUNJUNGAN (SELECT dari Canvas) ============
    public Page<CanvasVisitRecord> getAllCanvas(int page, int size,
                                                String startDateStr, String endDateStr,
                                                String search, String pelangganId, String kunjungan) {
        LocalDate start = (startDateStr != null && !startDateStr.isBlank())
                ? LocalDate.parse(startDateStr) : LocalDate.of(2000, 1, 1);
        LocalDate end = (endDateStr != null && !endDateStr.isBlank())
                ? LocalDate.parse(endDateStr) : LocalDate.now();
        LocalDate endExclusive = end.plusDays(1);

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> whereArgs = new ArrayList<>();
        where.append(" AND c.tanggal >= ? AND c.tanggal < ?");
        whereArgs.add(Timestamp.valueOf(start.atStartOfDay()));
        whereArgs.add(Timestamp.valueOf(endExclusive.atStartOfDay()));
        if (search != null && !search.isBlank()) {
            where.append(" AND LOWER(p.\"namaInstansi\") LIKE LOWER(?)");
            whereArgs.add("%" + search.trim() + "%");
        }
        if (pelangganId != null && !pelangganId.isBlank()) {
            where.append(" AND c.\"pelangganId\" = ?");
            whereArgs.add(pelangganId);
        }
        if (kunjungan != null && !kunjungan.isBlank()) {
            where.append(" AND LOWER(c.kunjungan) = LOWER(?)");
            whereArgs.add(kunjungan.trim());
        }

        Integer total = portalJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"Canvas\" c JOIN \"Pelanggan\" p ON p.id = c.\"pelangganId\"" + where,
                Integer.class, whereArgs.toArray());
        int totalInt = total == null ? 0 : total;

        StringBuilder sql = new StringBuilder(
                "SELECT c.id, c.\"pelangganId\", p.\"namaInstansi\", c.tanggal, c.kunjungan, c.keterangan, c.catatan "
                        + "FROM \"Canvas\" c JOIN \"Pelanggan\" p ON p.id = c.\"pelangganId\"")
                .append(where)
                .append(" ORDER BY c.tanggal DESC LIMIT ? OFFSET ?");
        List<Object> listArgs = new ArrayList<>(whereArgs);
        listArgs.add(size);
        listArgs.add((long) page * size);

        List<CanvasVisitRecord> rows = portalJdbcTemplate.query(sql.toString(), listArgs.toArray(), this::mapCanvas);

        Pageable pageable = PageRequest.of(page, size);
        if (totalInt == 0) return new PageImpl<>(rows, pageable, 0);
        if (page >= (int) Math.ceil(totalInt / (double) size)) {
            return getAllCanvas(0, size, startDateStr, endDateStr, search, pelangganId, kunjungan);
        }
        return new PageImpl<>(rows, pageable, totalInt);
    }

    private CanvasVisitRecord mapCanvas(ResultSet rs, int i) throws SQLException {
        Timestamp ts = rs.getTimestamp("tanggal");
        LocalDate d = ts == null ? null : ts.toLocalDateTime().toLocalDate();
        return CanvasVisitRecord.builder()
                .id(rs.getString("id"))
                .pelangganId(rs.getString("pelangganId"))
                .namaInstansi(rs.getString("namaInstansi"))
                .tanggalLabel(d == null ? "-" : formatDate(d))
                .kunjungan(rs.getString("kunjungan"))
                .keterangan(rs.getString("keterangan"))
                .catatan(rs.getString("catatan"))
                .build();
    }

    private String formatDate(LocalDate d) {
        return d.getDayOfMonth() + " " + MONTHS[d.getMonthValue() - 1] + " " + d.getYear();
    }
}