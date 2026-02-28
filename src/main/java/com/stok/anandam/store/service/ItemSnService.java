package com.stok.anandam.store.service;

import com.stok.anandam.store.dto.ItemSerialNumberResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ItemSnService {

    /** Field yang boleh dipakai untuk sortBy (aman dari SQL injection). */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("tanggal", "docId", "user", "itemName", "sn");

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Ambil data SN (Masuk/Keluar) dengan filter lengkap.
     * 
     * @param type      MASUK atau KELUAR
     * @param search    Pencarian global (sn, doc_id, user, item_name)
     * @param docId     Filter nomor dokumen (partial)
     * @param user      Filter nama user/par_name (partial)
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

        String dateColumn;
        String docCol;
        String userCol;
        if ("MASUK".equalsIgnoreCase(type)) {
            dateColumn = "p.doc_date";
            docCol = "p.doc_no";
            userCol = "p.par_name";
            sql.append(
                    "SELECT p.doc_date AS tanggal, p.doc_no AS doc_id, p.par_name AS user, m.name AS item_name, sn.sn ")
                    .append("FROM dbtitemsn sn ")
                    .append("LEFT JOIN dbmitem m ON sn.ite_id = m.id ")
                    .append("LEFT JOIN dbtpurchasedoc p ON sn.doc_id = p.id AND sn.doc_type = p.doc_type ")
                    .append("WHERE sn.doc_type IN (42,43,44) ");
        } else {
            dateColumn = "s.doc_date";
            docCol = "s.doc_no";
            userCol = "s.par_name";
            sql.append(
                    "SELECT s.doc_date AS tanggal, s.doc_no AS doc_id, s.par_name AS user, m.name AS item_name, sn.sn ")
                    .append("FROM dbtitemsn sn ")
                    .append("LEFT JOIN dbmitem m ON sn.ite_id = m.id ")
                    .append("LEFT JOIN dbtsalesdoc s ON sn.doc_id = s.id AND sn.doc_type = s.doc_type ")
                    .append("WHERE sn.doc_type IN (32,33) ");
        }

        applyFilters(sql, params, search, docId, user, itemName, sn, startDate, endDate, docCol, userCol, dateColumn);

        // Sort: whitelist field -> kolom asli (aman dari SQL injection)
        String orderColumn = dateColumn;
        if (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) {
            switch (sortBy) {
                case "docId":
                    orderColumn = docCol;
                    break;
                case "user":
                    orderColumn = userCol;
                    break;
                case "itemName":
                    orderColumn = "m.name";
                    break;
                case "sn":
                    orderColumn = "sn.sn";
                    break;
                case "tanggal":
                default:
                    orderColumn = dateColumn;
                    break;
            }
        }
        String dir = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
        sql.append("ORDER BY ").append(orderColumn).append(" ").append(dir).append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> ItemSerialNumberResponse.builder()
                .tanggal(rs.getTimestamp("tanggal") != null ? rs.getTimestamp("tanggal").toLocalDateTime() : null)
                .docId(rs.getString("doc_id"))
                .user(rs.getString("user"))
                .itemName(rs.getString("item_name"))
                .sn(rs.getString("sn"))
                .build());
    }

    public long countSnData(String type, String search, String docId, String user,
            String itemName, String sn, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        String dateColumn;
        String docCol;
        String userCol;
        if ("MASUK".equalsIgnoreCase(type)) {
            dateColumn = "p.doc_date";
            docCol = "p.doc_no";
            userCol = "p.par_name";
            sql.append("SELECT COUNT(*) ")
                    .append("FROM dbtitemsn sn ")
                    .append("LEFT JOIN dbmitem m ON sn.ite_id = m.id ")
                    .append("LEFT JOIN dbtpurchasedoc p ON sn.doc_id = p.id AND sn.doc_type = p.doc_type ")
                    .append("WHERE sn.doc_type IN (42,43,44) ");
        } else {
            dateColumn = "s.doc_date";
            docCol = "s.doc_no";
            userCol = "s.par_name";
            sql.append("SELECT COUNT(*) ")
                    .append("FROM dbtitemsn sn ")
                    .append("LEFT JOIN dbmitem m ON sn.ite_id = m.id ")
                    .append("LEFT JOIN dbtsalesdoc s ON sn.doc_id = s.id AND sn.doc_type = s.doc_type ")
                    .append("WHERE sn.doc_type IN (32,33) ");
        }

        applyFilters(sql, params, search, docId, user, itemName, sn, startDate, endDate, docCol, userCol, dateColumn);

        Long count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
        return count != null ? count : 0L;
    }

    private void applyFilters(StringBuilder sql, List<Object> params, String search, String docId, String user,
            String itemName, String sn, String startDate, String endDate, String docCol, String userCol,
            String dateColumn) {
        sql.append("AND sn.sn IS NOT NULL AND TRIM(sn.sn) <> '' ");

        // Filter: search global (semua field)
        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim() + "%";
            sql.append("AND (sn.sn LIKE ? OR ").append(docCol).append(" LIKE ? OR ").append(userCol)
                    .append(" LIKE ? OR m.name LIKE ?) ");
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        // Filter: docId (nomor dokumen)
        if (docId != null && !docId.isBlank()) {
            sql.append("AND ").append(docCol).append(" LIKE ? ");
            params.add("%" + docId.trim() + "%");
        }

        // Filter: user (nama user/par_name)
        if (user != null && !user.isBlank()) {
            sql.append("AND ").append(userCol).append(" LIKE ? ");
            params.add("%" + user.trim() + "%");
        }

        // Filter: itemName (nama barang)
        if (itemName != null && !itemName.isBlank()) {
            sql.append("AND m.name LIKE ? ");
            params.add("%" + itemName.trim() + "%");
        }

        // Filter: sn (serial number)
        if (sn != null && !sn.isBlank()) {
            sql.append("AND sn.sn LIKE ? ");
            params.add("%" + sn.trim() + "%");
        }

        // Filter: date range
        if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
            sql.append("AND (").append(dateColumn).append(" BETWEEN ? AND ?) ");
            params.add(startDate);
            params.add(endDate);
        }
    }
}