package com.stok.anandam.store.service;

import com.stok.anandam.store.dto.ReminderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Membaca & mengolah data Reminder dari database Portal (eksternal).
 */
@Service
public class ReminderService {

    private static final String[] MONTHS = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    @Autowired
    @Qualifier("portalJdbcTemplate")
    private JdbcTemplate portalJdbcTemplate;

    /**
     * Ambil reminder beserta nama instansi (JOIN ke Pelanggan), lalu hitung
     * hari tidak dikunjungi & selisih lewat batas agar siap ditampilkan.
     */
    public List<ReminderItem> getAllReminders() {
        String sql = "SELECT r.id, p.\"namaInstansi\", p.kategori, p.kabupaten, "
                + "r.\"lastCanvasAt\", r.\"intervalDays\" "
                + "FROM \"Reminder\" r "
                + "JOIN \"Pelanggan\" p ON p.id = r.\"pelangganId\" "
                + "ORDER BY r.\"lastCanvasAt\" ASC";
        return portalJdbcTemplate.query(sql, (rs, i) -> mapRow(rs));
    }

    private ReminderItem mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("lastCanvasAt");
        LocalDate lastDate = ts == null ? null : ts.toLocalDateTime().toLocalDate();
        int interval = rs.getInt("intervalDays");
        long days = (lastDate == null) ? 0 : Math.max(0, ChronoUnit.DAYS.between(lastDate, LocalDate.now()));
        long overdue = Math.max(0, days - interval);

        return ReminderItem.builder()
                .id(rs.getString("id"))
                .namaInstansi(rs.getString("namaInstansi"))
                .kategori(rs.getString("kategori"))
                .kabupaten(rs.getString("kabupaten"))
                .lastCanvasAtLabel(lastDate == null ? "-" : formatDate(lastDate))
                .daysNotVisited(days)
                .intervalDays(interval)
                .overdueDays(overdue)
                .build();
    }

    private String formatDate(LocalDate d) {
        return d.getDayOfMonth() + " " + MONTHS[d.getMonthValue() - 1] + " " + d.getYear();
    }
}