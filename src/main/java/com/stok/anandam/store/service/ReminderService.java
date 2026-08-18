package com.stok.anandam.store.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Membaca data Reminder dari database Portal (eksternal).
 */
@Service
public class ReminderService {

    @Autowired
    @Qualifier("portalJdbcTemplate")
    private JdbcTemplate portalJdbcTemplate;

    /**
     * Ambil seluruh baris dari tabel Reminder di database portal.
     * Nilai dikonversi ke String agar aman untuk serialisasi JSON.
     */
    public List<Map<String, Object>> getAllReminders() {
        List<Map<String, Object>> rows = portalJdbcTemplate.queryForList("SELECT * FROM Reminder");
        return rows.stream().map(row -> {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                Object v = e.getValue();
                normalized.put(e.getKey(), v == null ? null : v.toString());
            }
            return normalized;
        }).collect(Collectors.toList());
    }
}