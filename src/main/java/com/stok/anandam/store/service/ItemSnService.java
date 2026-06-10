package com.stok.anandam.store.service;

import com.stok.anandam.store.dto.ItemSerialNumberResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

@Service
public class ItemSnService {

    private static final Logger log = LoggerFactory.getLogger(ItemSnService.class);

    /** Field yang boleh dipakai untuk sortBy (aman dari SQL injection). */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("tanggal", "docId", "user", "itemName", "sn");

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Ambil data SN (Masuk/Keluar) dengan filter lengkap.
     * Data diambil dari tabel item_serial_numbers di PostgreSQL.
     *
     * @param type      MASUK atau KELUAR
     * @param search    Pencarian global (sn, doc_id, user_name, item_name)
     * @param docId     Filter nomor dokumen (partial)
     * @param user      Filter nama user/user_name (partial)
     * @param itemName  Filter nama barang (partial)
     * @param sn        Filter serial number (partial)
     * @param startDate Filter tanggal mulai (yyyy-MM-dd)
     * @param endDate   Filter tanggal akhir (yyyy-MM-dd)
     * @param sortBy    Field sort: tanggal, docId, user, itemName, sn
     * @param direction asc atau desc
     */
    public List<ItemSerialNumberResponse> getSnData(String type, String search, String docId, String user,
            String itemName, String sn, String startDate, String endDate,
            String sortBy, String direction, int size, int offset) {

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT tanggal, doc_id, user_name, item_name, sn ")
           .append("FROM item_serial_numbers ")
           .append("WHERE sn IS NOT NULL AND TRIM(sn) <> '' ");

        // Filter: type (MASUK / KELUAR)
        if (type != null && !type.isBlank()) {
            sql.append("AND UPPER(type) = ? ");
            params.add(type.toUpperCase());
        }

        applyFilters(sql, params, search, docId, user, itemName, sn, startDate, endDate);

        // Sort: whitelist field -> kolom asli (aman dari SQL injection)
        String orderColumn = "tanggal";
        if (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) {
            switch (sortBy) {
                case "docId":
                    orderColumn = "doc_id";
                    break;
                case "user":
                    orderColumn = "user_name";
                    break;
                case "itemName":
                    orderColumn = "item_name";
                    break;
                case "sn":
                    orderColumn = "sn";
                    break;
                case "tanggal":
                default:
                    orderColumn = "tanggal";
                    break;
            }
        }

        String dir = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        sql.append("ORDER BY ").append(orderColumn).append(" ").append(dir)
           .append(" NULLS LAST LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        log.debug("getSnData SQL: {}", sql);

        return jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> ItemSerialNumberResponse.builder()
                        .tanggal(rs.getObject("tanggal", LocalDateTime.class))
                        .docId(rs.getString("doc_id"))
                        .user(rs.getString("user_name"))
                        .itemName(rs.getString("item_name"))
                        .sn(rs.getString("sn"))
                        .build(),
                params.toArray());
    }

    public long countSnData(String type, String search, String docId, String user,
            String itemName, String sn, String startDate, String endDate) {

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT COUNT(*) FROM item_serial_numbers ")
           .append("WHERE sn IS NOT NULL AND TRIM(sn) <> '' ");

        // Filter: type (MASUK / KELUAR)
        if (type != null && !type.isBlank()) {
            sql.append("AND UPPER(type) = ? ");
            params.add(type.toUpperCase());
        }

        applyFilters(sql, params, search, docId, user, itemName, sn, startDate, endDate);

        log.debug("countSnData SQL: {}", sql);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    private void applyFilters(StringBuilder sql, List<Object> params, String search, String docId, String user,
            String itemName, String sn, String startDate, String endDate) {

        // Filter: search global (semua field)
        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(sn) LIKE ? OR LOWER(doc_id) LIKE ? OR LOWER(user_name) LIKE ? OR LOWER(item_name) LIKE ?) ");
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        // Filter: docId (nomor dokumen)
        if (docId != null && !docId.isBlank()) {
            sql.append("AND LOWER(doc_id) LIKE ? ");
            params.add("%" + docId.trim().toLowerCase() + "%");
        }

        // Filter: user (nama user/user_name)
        if (user != null && !user.isBlank()) {
            sql.append("AND LOWER(user_name) LIKE ? ");
            params.add("%" + user.trim().toLowerCase() + "%");
        }

        // Filter: itemName (nama barang)
        if (itemName != null && !itemName.isBlank()) {
            sql.append("AND LOWER(item_name) LIKE ? ");
            params.add("%" + itemName.trim().toLowerCase() + "%");
        }

        // Filter: sn (serial number)
        if (sn != null && !sn.isBlank()) {
            sql.append("AND LOWER(sn) LIKE ? ");
            params.add("%" + sn.trim().toLowerCase() + "%");
        }

        // Filter: date range
        if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
            sql.append("AND (tanggal BETWEEN ?::timestamp AND ?::timestamp) ");
            params.add(startDate + " 00:00:00");
            params.add(endDate + " 23:59:59");
        }
    }
}