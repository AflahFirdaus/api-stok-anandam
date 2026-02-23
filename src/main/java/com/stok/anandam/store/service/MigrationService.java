package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Sales;
import com.stok.anandam.store.core.postgres.model.Stock;
import com.stok.anandam.store.core.postgres.model.Tkdn;
import com.stok.anandam.store.core.postgres.model.Purchase;
import com.stok.anandam.store.core.postgres.repository.PurchaseRepository;
import com.stok.anandam.store.core.postgres.repository.SalesRepository;
import com.stok.anandam.store.core.postgres.repository.StockRepository;
import com.stok.anandam.store.core.postgres.repository.TkdnRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.stok.anandam.store.core.postgres.model.Canvasing;
import com.stok.anandam.store.core.postgres.repository.CanvasingRepository;
import org.springframework.core.io.ClassPathResource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnProperty(name = "app.mysql.enabled", havingValue = "true")
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);

    @Autowired
    @org.springframework.context.annotation.Lazy
    private MigrationService self;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    @Qualifier("legacyJdbcTemplate")
    private JdbcTemplate legacyJdbcTemplate;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int BATCH_SIZE = 1000;

    private static final String SQL_PURCHASE = """
                SELECT
                    d.doc_date, d.doc_no, m.code AS code, d.par_name,
                    dept.code AS dep_code, m.code AS item_code, m.name AS item_name,
                    CASE WHEN d.doc_no LIKE '%%RB%%' THEN -t.qty_def ELSE t.qty_def END AS qty_def,
                    t.price,
                    (CASE WHEN d.doc_no LIKE '%%RB%%' THEN -t.qty_def ELSE t.qty_def END * t.price) AS grand_total
                FROM dbtpurchasedoc d
                LEFT JOIN dbtpurchasetrans t ON d.id = t.doc_id
                LEFT JOIN dbmitem m ON t.ite_id = m.id
                LEFT JOIN dbmdepartment dept ON m.dep_id = dept.id
                ORDER BY d.doc_date DESC, d.id DESC
            """;

    private String getSqlPurchase() {
        return SQL_PURCHASE;
    }

    @Async
    public CompletableFuture<String> migratePurchaseData() {
        long startTime = System.currentTimeMillis();

        log.info("=== START MIGRASI PURCHASE ===");

        try {
            // 0. HITUNG ESTIMASI DATA (pakai schema dari config agar data terbaru)
            String countSql = "SELECT COUNT(*) FROM (" + getSqlPurchase() + ") as total";
            try {
                Integer totalRows = legacyJdbcTemplate.queryForObject(countSql, Integer.class);
                log.info("ESTIMASI TOTAL DATA PURCHASE DARI SOURCE: {}", totalRows);
            } catch (Exception e) {
                log.warn("Gagal menghitung total data source: {}", e.getMessage());
            }

            // 1. BERSIHKAN DATA LAMA DULU (TRUNCATE)
            log.info("Membersihkan tabel Purchase di PostgreSQL...");
            resetTable();
            log.info("Tabel bersih. ID di-reset ke 1.");

            final List<Purchase> buffer = new ArrayList<>();
            final int[] totalProcessed = { 0 };

            // 2. MULAI STREAMING DATA BARU (ORDER BY doc_date DESC = terbaru dulu)
            legacyJdbcTemplate.query(getSqlPurchase(), new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    try {
                        Purchase p = new Purchase();

                        // Mapping
                        java.sql.Date sqlDate = rs.getDate("doc_date");
                        if (sqlDate != null)
                            p.setDocDate(sqlDate.toLocalDate());
                        p.setDocNoP(rs.getString("doc_no"));
                        p.setParName(rs.getString("par_name"));
                        p.setDepCode(rs.getString("dep_code"));
                        p.setItemCode(rs.getString("item_code"));
                        p.setItemName(rs.getString("item_name"));
                        p.setQty(rs.getInt("qty_def"));
                        p.setPrice(rs.getBigDecimal("price"));
                        p.setGrandTotal(rs.getBigDecimal("grand_total"));

                        buffer.add(p); // Tambah data ke buffer

                        // Cek apakah buffer sudah penuh?
                        if (buffer.size() >= BATCH_SIZE) {
                            // 1. HITUNG DULU SEBELUM DIHAPUS (FIX DISINI)
                            totalProcessed[0] += buffer.size();

                            // 2. BARU SIMPAN (Ini akan mengosongkan buffer)
                            self.saveBatch(buffer);

                            log.info("Migrated: {} data...", totalProcessed[0]);
                        }
                    } catch (Exception e) {
                        log.warn("Error processing row: {}", e.getMessage());
                    }
                }
            });

            // Di luar looping (sisa data < 1000)
            if (!buffer.isEmpty()) {
                totalProcessed[0] += buffer.size(); // Hitung dulu
                self.saveBatch(buffer); // Baru simpan
            }

            long duration = System.currentTimeMillis() - startTime;
            String result = "=== SELESAI === Total Data: " + totalProcessed[0] + ". Waktu: " + (duration / 1000)
                    + " detik.";
            log.info("{}", result);

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("CRITICAL ERROR during Purchase Data Migration: {}", e.getMessage(), e);
            logConnectionCause(e);
            return CompletableFuture.completedFuture("ERROR: " + e.getMessage());
        }
    }

    // Helper: Reset Table (Dipisah biar Transaction-nya jelas)
    // Tidak perlu @Transactional di sini karena sudah ada di Repository
    public void resetTable() {
        purchaseRepository.truncateTable();
    }

    @Transactional
    public void saveBatch(List<Purchase> purchases) {
        String sql = """
                    INSERT INTO purchases (id, doc_date, doc_no_p, par_name, dep_code, item_code, item_name, qty, price, grand_total)
                    VALUES (nextval('purchase_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                Purchase p = purchases.get(i);
                ps.setDate(1, java.sql.Date.valueOf(p.getDocDate()));
                ps.setString(2, p.getDocNoP());
                ps.setString(3, p.getParName());
                ps.setString(4, p.getDepCode());
                ps.setString(5, p.getItemCode());
                ps.setString(6, p.getItemName());
                ps.setInt(7, p.getQty());
                ps.setBigDecimal(8, p.getPrice());
                ps.setBigDecimal(9, p.getGrandTotal());
            }

            @Override
            public int getBatchSize() {
                return purchases.size();
            }
        });
        purchases.clear();
    }

    @Autowired
    private SalesRepository salesRepository;

    private static final String SQL_SALES = """
                SELECT
                    d.doc_date,
                    d.doc_no,
                    p.code,
                    d.par_name,
                    t.ite_name,
                    CASE
                        WHEN d.doc_no LIKE '%%RJ%%' THEN -t.qty_def
                        ELSE t.qty_def
                    END AS qty_def,
                    t.price,
                    (
                        CASE
                            WHEN d.doc_no LIKE '%%RJ%%' THEN -t.qty_def
                            ELSE t.qty_def
                        END * t.price
                    ) AS grand_total,
                    e.code AS emp_code
                FROM dbtsalesdoc d
                LEFT JOIN dbtsalestrans t ON d.id = t.doc_id
                LEFT JOIN dbmemployee e ON d.emp_id = e.id
                LEFT JOIN dbmpartner p ON d.par_id = p.id
                ORDER BY d.doc_date DESC, d.id DESC
            """;

    private String getSqlSales() {
        return SQL_SALES;
    }

    @Async
    public CompletableFuture<String> migrateSalesData() {
        long startTime = System.currentTimeMillis();

        log.info("=== START MIGRASI SALES ===");

        try {
            // 0. HITUNG ESTIMASI DATA (schema dari config)
            String countSql = "SELECT COUNT(*) FROM (" + getSqlSales() + ") as total";
            try {
                Integer totalRows = legacyJdbcTemplate.queryForObject(countSql, Integer.class);
                log.info("ESTIMASI TOTAL DATA SALES DARI SOURCE: {}", totalRows);
            } catch (Exception e) {
                log.warn("Gagal menghitung total data source: {}", e.getMessage());
            }

            // 1. Bersihkan Tabel
            log.info("Membersihkan tabel Sales...");
            salesRepository.truncateTable();

            final List<Sales> buffer = new ArrayList<>();
            final int[] totalProcessed = { 0 };

            // 2. Streaming Data (data terbaru dulu)
            legacyJdbcTemplate.query(getSqlSales(), new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    try {
                        Sales s = new Sales();

                        // Mapping Data
                        java.sql.Date sqlDate = rs.getDate("doc_date");
                        if (sqlDate != null)
                            s.setDocDate(sqlDate.toLocalDate());

                        s.setDocNo(rs.getString("doc_no"));
                        s.setCode(rs.getString("code")); // p.code
                        s.setParName(rs.getString("par_name"));
                        s.setItemName(rs.getString("ite_name")); // t.ite_name

                        // Logic minus/plus sudah dihandle di SQL Query (CASE WHEN)
                        // Jadi di Java tinggal ambil nilainya saja
                        s.setQty(rs.getInt("qty_def"));
                        s.setPrice(rs.getBigDecimal("price"));
                        s.setGrandTotal(rs.getBigDecimal("grand_total"));

                        s.setEmpCode(rs.getString("emp_code"));

                        buffer.add(s);

                        // Batch Save
                        if (buffer.size() >= BATCH_SIZE) {
                            totalProcessed[0] += buffer.size(); // Hitung dulu (FIX BUG 0)
                            self.saveSalesBatch(buffer); // Simpan & Clear
                            log.info("Sales Migrated: {}...", totalProcessed[0]);
                        }
                    } catch (Exception e) {
                        log.warn("Error processing Sales row: {}", e.getMessage());
                    }
                }
            });

            // Sisa Data
            if (!buffer.isEmpty()) {
                totalProcessed[0] += buffer.size();
                self.saveSalesBatch(buffer);
            }

            long duration = System.currentTimeMillis() - startTime;
            String result = "=== SALES SELESAI === Total: " + totalProcessed[0] + ". Waktu: " + (duration / 1000)
                    + " detik.";
            log.info("{}", result);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("CRITICAL ERROR during Sales Data Migration: {}", e.getMessage(), e);
            logConnectionCause(e);
            return CompletableFuture.completedFuture("ERROR: " + e.getMessage());
        }
    }

    @Transactional
    public void saveSalesBatch(List<Sales> salesList) {
        String sql = """
                    INSERT INTO sales (id, doc_date, doc_no, code, par_name, item_name, qty, price, grand_total, emp_code)
                    VALUES (nextval('sales_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                Sales s = salesList.get(i);
                ps.setDate(1, java.sql.Date.valueOf(s.getDocDate()));
                ps.setString(2, s.getDocNo());
                ps.setString(3, s.getCode());
                ps.setString(4, s.getParName());
                ps.setString(5, s.getItemName());
                ps.setInt(6, s.getQty());
                ps.setBigDecimal(7, s.getPrice());
                ps.setBigDecimal(8, s.getGrandTotal());
                ps.setString(9, s.getEmpCode());
            }

            @Override
            public int getBatchSize() {
                return salesList.size();
            }
        });
        salesList.clear();
    }

    @Autowired
    private StockRepository stockRepository;

    // SQL Stok: pakai schema default dari connection string
    private static final String SQL_STOCK = """
                SELECT
                    i.code AS item_code,
                    i.name AS item_name,
                    SUBSTRING_INDEX(i.code, ' ', 1) AS kategori_itemcode,
                    SUBSTRING_INDEX(i.name, ' ', 1) AS kategori_nama,
                    stk.final_stock,
                    COALESCE(h.price_avg, 0) AS harga_hpp,
                    (stk.final_stock * COALESCE(h.price_avg, 0)) AS grand_total,
                    w.name AS warehouse_name
                FROM (
                    SELECT
                        war_id,
                        ite_id,
                        SUM(qty_movement) AS final_stock
                    FROM (
                        /* PURCHASE */
                        SELECT d.war_id, t.ite_id,
                            CASE WHEN d.doc_no LIKE 'BL%%' THEN t.qty_def
                                 WHEN d.doc_no LIKE 'RB%%' THEN -t.qty_def
                                 ELSE 0 END AS qty_movement
                        FROM dbtpurchasedoc d JOIN dbtpurchasetrans t ON d.id = t.doc_id

                        UNION ALL

                        /* TRANSFER */
                        SELECT d.war_id, t.ite_id,
                            CASE WHEN d.doc_no LIKE 'II%%' THEN t.qty_def
                                 WHEN d.doc_no LIKE 'IO%%' THEN -t.qty_def
                                 ELSE 0 END AS qty_movement
                        FROM dbtitemtransferdoc d JOIN dbtitemtransfertrans t ON d.id = t.doc_id

                        UNION ALL

                        /* SALES */
                        SELECT d.war_id, t.ite_id,
                            CASE WHEN d.doc_no LIKE 'JL%%' THEN -t.qty_def
                                 WHEN d.doc_no LIKE 'RJ%%' THEN t.qty_def
                                 ELSE 0 END AS qty_movement
                        FROM dbtsalesdoc d JOIN dbtsalestrans t ON d.id = t.doc_id
                    ) trans
                    GROUP BY war_id, ite_id
                ) stk
                JOIN dbmitem i ON stk.ite_id = i.id
                JOIN dbmwarehouse w ON stk.war_id = w.id
                LEFT JOIN (
                    SELECT s1.ite_id, s1.price_avg
                    FROM dbtstockavg s1
                    JOIN (SELECT ite_id, MAX(id) as max_id FROM dbtstockavg GROUP BY ite_id) s2
                    ON s1.ite_id = s2.ite_id AND s1.id = s2.max_id
                ) h ON stk.ite_id = h.ite_id
                ORDER BY i.name
            """;

    private String getSqlStock() {
        return SQL_STOCK;
    }

    @Async
    public CompletableFuture<String> migrateStockData() {
        long startTime = System.currentTimeMillis();
        log.info("=== START MIGRASI STOK ===");

        try {
            log.info("Membersihkan tabel Stok...");
            stockRepository.truncateTable();

            final List<Stock> buffer = new ArrayList<>();
            final int[] totalProcessed = { 0 };

            legacyJdbcTemplate.query(getSqlStock(), new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    try {
                        Stock s = new Stock();

                        String code = rs.getString("item_code");
                        String name = rs.getString("item_name");

                        s.setItemCode(code);
                        s.setItemName(name);

                        // === LOGIC UPDATE KATEGORI ===

                        // 1. Kategori dari ITEM CODE (Split berdasarkan strip '-')
                        // Contoh: "LCD-AC_22..." -> diambil "LCD"
                        if (code != null && !code.isEmpty()) {
                            // Cek apakah ada strip "-", jika ada ambil depannya.
                            // Jika tidak ada strip, ambil kata pertama (spasi) atau ambil semuanya.
                            if (code.contains("-")) {
                                s.setKategoriItemcode(code.split("-")[0]);
                            } else {
                                s.setKategoriItemcode(code.split(" ")[0]);
                            }
                        }

                        // 2. Kategori dari ITEM NAME (Split berdasarkan spasi ' ')
                        // Contoh: "KULKAS 2 PINTU" -> diambil "KULKAS"
                        if (name != null && !name.isEmpty()) {
                            s.setKategoriNama(name.split(" ")[0]);
                        }

                        s.setFinalStok(rs.getInt("final_stock"));
                        s.setHargaHpp(rs.getBigDecimal("harga_hpp"));
                        s.setGrandTotal(rs.getBigDecimal("grand_total"));
                        s.setWarehouse(rs.getString("warehouse_name"));

                        buffer.add(s);

                        if (buffer.size() >= BATCH_SIZE) {
                            totalProcessed[0] += buffer.size();
                            self.saveStockBatch(buffer); // Panggil method batch insert kamu
                            log.info("Stock Migrated: {}...", totalProcessed[0]);
                        }
                    } catch (Exception e) {
                        log.warn("Error processing Stock row: {}", e.getMessage());
                    }
                }
            });

            if (!buffer.isEmpty()) {
                totalProcessed[0] += buffer.size();
                self.saveStockBatch(buffer);
            }

            // Isi spesifikasi, modal, final pricelist dari sheet PRICELIST&MODAL (fill-down kosong)
            String syncResult = self.syncStockPricelistFromSheet();
            log.info("Sync Pricelist dari Sheet: {}", syncResult);

            long duration = System.currentTimeMillis() - startTime;
            String result = "=== STOK SELESAI === Total: " + totalProcessed[0] + ". " + syncResult + " Waktu: " + (duration / 1000)
                    + " detik.";
            log.info("{}", result);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("CRITICAL ERROR during Stock Data Migration: {}", e.getMessage(), e);
            logConnectionCause(e);
            return CompletableFuture.completedFuture("ERROR: " + e.getMessage());
        }
    }

    /**
     * Baca sheet PRICELIST&MODAL, isi kolom kosong dengan nilai dari baris atas (fill-down untuk spesifikasi & pricelist),
     * lalu update stok by item name.
     */
    @Transactional
    public String syncStockPricelistFromSheet() {
        try {
            InputStream in = MigrationService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) return "ERROR: service_account.json tidak ditemukan";

            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
            Sheets service = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Anandam Store")
                    .build();

            ValueRange response = service.spreadsheets().values().get(SPREADSHEET_ID, PRICELIST_RANGE).execute();
            List<List<Object>> values = response.getValues();
            if (values == null || values.size() < 2) return "Sheet kosong atau hanya header";

            // Header bisa tidak berada di baris pertama. Cari dulu baris header.
            int headerRowIndex = -1;
            Map<String, Integer> headerMap = new HashMap<>();
            for (int r = 0; r < values.size(); r++) {
                List<Object> possibleHeader = values.get(r);
                Map<String, Integer> probe = new HashMap<>();
                for (int c = 0; c < possibleHeader.size(); c++) {
                    probe.put(possibleHeader.get(c).toString().trim().toUpperCase(), c);
                }
                Integer probeItem = getHeaderIndexFirst(probe, "NAMA BARANG", "ITEM NAME", "NAMA", "ITEM");
                Integer probeSpec = getHeaderIndexFirst(probe, "SPESIFIKASI", "SPESIFIKASI LENGKAP");
                Integer probeModal = getHeaderIndexFirst(probe, "MODAL", "MODAL FINAL");
                Integer probePrice = getHeaderIndexFirst(probe, "FINAL PRICELIST", "PRICELIST", "HARGA JUAL", "PRICE");
                // Anggap valid jika minimal item + salah satu kolom harga/spec ada
                if (probeItem != null && (probeSpec != null || probeModal != null || probePrice != null)) {
                    headerRowIndex = r;
                    headerMap = probe;
                    break;
                }
            }

            Integer idxItemName;
            Integer idxItemCode = null;
            Integer idxSpesifikasi;
            Integer idxModal;
            Integer idxPricelist;
            if (headerRowIndex >= 0) {
                log.info("PRICELIST&MODAL header row index {}: {}", headerRowIndex, headerMap.keySet());
                idxItemName = getHeaderIndexFirst(headerMap, "NAMA BARANG", "ITEM NAME", "NAMA", "ITEM");
                idxItemCode = getHeaderIndexFirst(headerMap, "ITEM CODE", "KODE ITEM", "CODE", "SKU");
                idxSpesifikasi = getHeaderIndexFirst(headerMap, "SPESIFIKASI", "SPESIFIKASI LENGKAP");
                idxModal = getHeaderIndexFirst(headerMap, "MODAL", "MODAL FINAL");
                idxPricelist = getHeaderIndexFirst(headerMap, "FINAL PRICELIST", "PRICELIST", "HARGA JUAL", "PRICE");
            } else {
                // Fallback: tidak ada header -> pakai urutan yang Anda kasih:
                // A: Spesifikasi | B: Item Name | C: Modal Final | D: Pricelist
                log.info("PRICELIST&MODAL header tidak ditemukan, pakai fallback kolom A-D.");
                idxSpesifikasi = 0;
                idxItemName = 1;
                idxItemCode = 1; // fallback: asumsikan item code sama dengan item name jika tidak ada kolom khusus
                idxModal = 2;
                idxPricelist = 3;
            }

            if (idxItemName == null) {
                return "ERROR: Kolom nama item tidak ditemukan (header/fallback gagal).";
            }

            // Fill-down: nilai kosong diisi dari baris atas (HANYA spesifikasi & pricelist)
            String lastSpesifikasi = null;
            BigDecimal lastPricelist = null;
            Map<String, Object[]> byItemName = new HashMap<>(); // normalized key -> [spesifikasi, modal, finalPricelist]
            List<Object[]> orderedSheetRows = new ArrayList<>(); // urutan asli sheet -> [itemName, spesifikasi, modal, pricelist]

            for (int i = 0; i < values.size(); i++) {
                if (i == headerRowIndex) continue; // skip baris header (jika ada)
                List<Object> row = values.get(i);
                String itemName = getValByIndex(row, idxItemName);
                String itemCode = getValByIndex(row, idxItemCode);
                String fallbackNameColA = (headerRowIndex < 0) ? getValByIndex(row, 0) : null;
                String spesifikasiRaw = getValByIndex(row, idxSpesifikasi);
                String modalStr = getValByIndex(row, idxModal);
                String pricelistStr = getValByIndex(row, idxPricelist);

                // Fill-down: kosong pakai nilai baris atas (spesifikasi & pricelist)
                if (spesifikasiRaw != null && !spesifikasiRaw.isBlank()) lastSpesifikasi = spesifikasiRaw;
                String spesifikasi = (spesifikasiRaw != null && !spesifikasiRaw.isBlank()) ? spesifikasiRaw : lastSpesifikasi;

                // Modal TIDAK fill-down: jika kosong tetap null
                BigDecimal modal = (modalStr != null && !modalStr.isBlank()) ? cleanBigDecimal(modalStr) : null;

                if (pricelistStr != null && !pricelistStr.isBlank()) lastPricelist = cleanBigDecimal(pricelistStr);
                BigDecimal pricelist = (pricelistStr != null && !pricelistStr.isBlank()) ? cleanBigDecimal(pricelistStr) : lastPricelist;

                Object[] payload = new Object[]{ spesifikasi, modal, pricelist };
                if (itemName != null && !itemName.isBlank()) {
                    byItemName.put(normalizeItemName(itemName), payload);
                }
                if (itemCode != null && !itemCode.isBlank()) {
                    byItemName.put(normalizeItemName(itemCode), payload);
                }
                // Fallback penting untuk format tanpa header: kolom A kadang berisi nama item versi panjang
                if (fallbackNameColA != null && !fallbackNameColA.isBlank()) {
                    byItemName.put(normalizeItemName(fallbackNameColA), payload);
                }

                // Simpan urutan baris sheet untuk fallback by-order
                if (itemName != null && !itemName.isBlank()) {
                    orderedSheetRows.add(new Object[] { itemName, spesifikasi, modal, pricelist });
                }
            }

            List<Stock> allStocks = stockRepository.findAll();
            String normKey = null;
            int updated = 0;
            int fuzzyMatched = 0;
            for (Stock s : allStocks) {
                if (s.getItemName() == null && s.getItemCode() == null) continue;

                // 1) Exact by item_name
                normKey = normalizeItemName(s.getItemName());
                Object[] row = byItemName.get(normKey);

                // 2) Exact by item_code
                if (row == null && s.getItemCode() != null) {
                    row = byItemName.get(normalizeItemName(s.getItemCode()));
                }

                // 3) Fuzzy by item_name (contains dua arah + compact compare)
                if (row == null && s.getItemName() != null) {
                    String stockNorm = normalizeItemName(s.getItemName());
                    String stockCompact = normalizeCompact(stockNorm);
                    for (Map.Entry<String, Object[]> e : byItemName.entrySet()) {
                        String sheetNorm = e.getKey();
                        String sheetCompact = normalizeCompact(sheetNorm);
                        if (stockNorm.contains(sheetNorm) || sheetNorm.contains(stockNorm)
                                || stockCompact.contains(sheetCompact) || sheetCompact.contains(stockCompact)) {
                            row = e.getValue();
                            fuzzyMatched++;
                            break;
                        }
                    }
                }

                if (row == null) continue;
                s.setSpesifikasi((String) row[0]);
                s.setModal(row[1] != null ? (BigDecimal) row[1] : null);
                s.setFinalPricelist(row[2] != null ? (BigDecimal) row[2] : null);
                updated++;
            }

            // Fallback: jika tidak ada yang match by-name, pakai urutan baris (sheet -> stok).
            if (updated == 0 && !allStocks.isEmpty() && !orderedSheetRows.isEmpty()) {
                List<Stock> orderedStocks = new ArrayList<>(allStocks);
                orderedStocks.sort(Comparator.comparing(Stock::getId, Comparator.nullsLast(Long::compareTo)));

                int limit = Math.min(orderedStocks.size(), orderedSheetRows.size());
                for (int i = 0; i < limit; i++) {
                    Stock s = orderedStocks.get(i);
                    Object[] sheet = orderedSheetRows.get(i);
                    // Sesuai permintaan: sesuaikan item_name berdasarkan urutan data sheet.
                    s.setItemName((String) sheet[0]);
                    s.setSpesifikasi((String) sheet[1]);
                    s.setModal(sheet[2] != null ? (BigDecimal) sheet[2] : null);
                    s.setFinalPricelist(sheet[3] != null ? (BigDecimal) sheet[3] : null);
                    updated++;
                }
                log.info("Fallback by-order aktif: {} stok diisi berdasarkan urutan baris sheet.", limit);
            }

            stockRepository.saveAll(allStocks);
            log.info("Pricelist mapping keys total: {}", byItemName.size());
            log.info("Pricelist fuzzy matched count: {}", fuzzyMatched);
            return "Pricelist sync: " + updated + " stok di-update dari sheet.";
        } catch (Exception e) {
            log.error("Error sync pricelist from sheet: {}", e.getMessage(), e);
            return "ERROR sync: " + e.getMessage();
        }
    }

    private static String normalizeItemName(String name) {
        if (name == null) return "";
        return name
                .toUpperCase()
                .replace("™", "")
                .replace("®", "")
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeCompact(String value) {
        if (value == null) return "";
        return value.replaceAll("[^A-Z0-9]", "");
    }

    private static Integer getHeaderIndexFirst(Map<String, Integer> headerMap, String... names) {
        for (String n : names) {
            if (headerMap.containsKey(n)) return headerMap.get(n);
        }
        return null;
    }

    private static String getValByIndex(List<Object> row, Integer index) {
        if (index == null) return null;
        if (index >= row.size()) return null;
        Object v = row.get(index);
        return (v == null) ? null : v.toString().trim();
    }

    private BigDecimal cleanBigDecimal(String val) {
        if (val == null || val.isBlank()) return null;
        String s = val.replace("\u00A0", "").replace(" ", "").replace("Rp", "");
        if (s.contains(".") && !s.contains(",")) s = s.replace(".", "");
        else if (s.contains(".") && s.contains(",")) s = s.split(",")[0].replace(".", "");
        s = s.replaceAll("[^0-9.-]", "");
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void saveStockBatch(List<Stock> stockList) {
        String sql = """
                    INSERT INTO stok (id, item_code, item_name, kategori_nama, kategori_itemcode, final_stok, harga_hpp, grand_total, warehouse)
                    VALUES (nextval('stok_seq'), ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                Stock s = stockList.get(i);
                ps.setString(1, s.getItemCode());
                ps.setString(2, s.getItemName());
                ps.setString(3, s.getKategoriNama());
                ps.setString(4, s.getKategoriItemcode());

                // Handle null safe for Integer
                if (s.getFinalStok() != null) {
                    ps.setInt(5, s.getFinalStok());
                } else {
                    ps.setNull(5, java.sql.Types.INTEGER);
                }

                ps.setBigDecimal(6, s.getHargaHpp());
                ps.setBigDecimal(7, s.getGrandTotal());
                ps.setString(8, s.getWarehouse());
            }

            @Override
            public int getBatchSize() {
                return stockList.size();
            }
        });
        stockList.clear();
    }

    @Async
    public CompletableFuture<String> migrateSnData() {
        log.info("=== START MIGRASI SERIAL NUMBER ===");
        try {
            // 1. Bersihkan tabel tujuan di Postgres (Pastikan kamu sudah buat repository-nya)
            pgJdbcTemplate.execute("TRUNCATE TABLE item_serial_numbers RESTART IDENTITY");

            // 2. Query Gabungan (Masuk & Keluar) dari Legacy MySQL
            String sqlSource = """
                SELECT p.doc_date AS tanggal, p.doc_no AS doc_id, p.par_name AS user_name, m.name AS item_name, sn.sn, 'MASUK' as type
                FROM dbtitemsn sn
                LEFT JOIN dbmitem m ON sn.ite_id = m.id
                LEFT JOIN dbtpurchasedoc p ON sn.doc_id = p.id AND sn.doc_type = p.doc_type
                WHERE sn.doc_type IN (42,43,44) AND sn.sn IS NOT NULL AND TRIM(sn.sn) <> ''
                UNION ALL
                SELECT s.doc_date AS tanggal, s.doc_no AS doc_id, s.par_name AS user_name, m.name AS item_name, sn.sn, 'KELUAR' as type
                FROM dbtitemsn sn
                LEFT JOIN dbmitem m ON sn.ite_id = m.id
                LEFT JOIN dbtsalesdoc s ON sn.doc_id = s.id AND sn.doc_type = s.doc_type
                WHERE sn.doc_type IN (32,33) AND sn.sn IS NOT NULL AND TRIM(sn.sn) <> ''
            """;

            final List<Object[]> buffer = new ArrayList<>();
            legacyJdbcTemplate.query(sqlSource, rs -> {
                Object[] row = new Object[] {
                    rs.getDate("tanggal"),
                    rs.getString("doc_id"),
                    rs.getString("user_name"),
                    rs.getString("item_name"),
                    rs.getString("sn"),
                    rs.getString("type")
                };
                buffer.add(row);
                if (buffer.size() >= BATCH_SIZE) {
                    saveSnBatch(new ArrayList<>(buffer));
                    buffer.clear();
                }
            });
            
            if (!buffer.isEmpty()) saveSnBatch(buffer);

            return CompletableFuture.completedFuture("Migrasi SN Selesai.");
        } catch (Exception e) {
            log.error("Error SN Migration: {}", e.getMessage());
            logConnectionCause(e);
            return CompletableFuture.completedFuture("Error: " + e.getMessage());
        }
    }

    /** Log penyebab koneksi JDBC (root cause) agar mudah debug MySQL/Postgres. */
    private void logConnectionCause(Throwable e) {
        Throwable cause = e;
        int depth = 0;
        while (cause != null && depth < 10) {
            log.error("Caused by [{}]: {} - {}", depth, cause.getClass().getSimpleName(), cause.getMessage());
            if (cause instanceof java.sql.SQLException) {
                java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
                if (sqlEx.getSQLState() != null) log.error("  SQLState: {}", sqlEx.getSQLState());
                if (sqlEx.getErrorCode() != 0) log.error("  ErrorCode: {}", sqlEx.getErrorCode());
            }
            cause = cause.getCause();
            depth++;
        }
    }

    private void saveSnBatch(List<Object[]> data) {
        String sqlInsert = "INSERT INTO item_serial_numbers (tanggal, doc_id, user_name, item_name, sn, type) VALUES (?, ?, ?, ?, ?, ?)";
        pgJdbcTemplate.batchUpdate(sqlInsert, data, new int[] { 
            java.sql.Types.DATE, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, 
            java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR 
        });
    }

    @Autowired
    private CanvasingRepository canvasingRepository;

    @Async
    public CompletableFuture<String> migrateCanvasingData() {
        long startTime = System.currentTimeMillis();
        log.info("=== START MIGRASI CANVASING (OPTIMIZED) ===");

        // 1. Truncate (Bersihkan tabel)
        log.info("Membersihkan tabel Canvasing...");
        canvasingRepository.truncateTable();

        final List<Canvasing> buffer = new ArrayList<>();
        final int[] totalProcessed = { 0 };

        try {
            // Baca file dari resources
            ClassPathResource resource = new ClassPathResource("canvasing.csv");
            if (!resource.exists()) {
                return CompletableFuture.completedFuture("ERROR: File canvasing.csv tidak ditemukan!");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                } // Skip Header

                // Split CSV sederhana
                // Hati-hati: Jika isi data mengandung koma, logic ini perlu library OpenCSV.
                // Tapi untuk data sederhana, split(",") sudah cukup.
                String[] data = line.split(",");

                if (data.length >= 2) {
                    Canvasing c = new Canvasing();

                    // Logic mapping safe (menghindari error array out of bounds)
                    c.setKategori(safeGet(data, 0));
                    c.setNamaInstansi(safeGet(data, 1));
                    c.setProvinsi(safeGet(data, 2));
                    c.setKabupaten(safeGet(data, 3));
                    c.setKecamatan(safeGet(data, 4));

                    // Filter: Jika nama instansi kosong, skip (sesuai logic Python kamu)
                    if (c.getNamaInstansi() != null && !c.getNamaInstansi().isEmpty()) {
                        buffer.add(c);
                    }
                }

                // Cek Buffer
                if (buffer.size() >= BATCH_SIZE) {
                    totalProcessed[0] += buffer.size();
                    self.saveCanvasingBatch(buffer); // Pake JDBC Template
                    log.info("Canvasing Migrated: {}...", totalProcessed[0]);
                }
            }
            reader.close();

            // Sisa Data
            if (!buffer.isEmpty()) {
                totalProcessed[0] += buffer.size();
                self.saveCanvasingBatch(buffer);
            }

        } catch (Exception e) {
            log.error("Error during Canvasing migration: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture("ERROR: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        String result = "=== CANVASING SELESAI === Total: " + totalProcessed[0] + ". Waktu: " + (duration / 1000)
                + " detik.";
        log.info("{}", result);
        return CompletableFuture.completedFuture(result);
    }

    // Helper sederhana untuk ambil data CSV aman
    private String safeGet(String[] data, int index) {
        if (index < data.length && data[index] != null) {
            // Bersihkan tanda kutip (") jika ada, dan trim spasi
            return data[index].replace("\"", "").trim();
        }
        return null;
    }

    // === INI BAGIAN KUNCI PERCEPATANNYA ===
    @Transactional
    public void saveCanvasingBatch(List<Canvasing> list) {
        String sql = """
                    INSERT INTO canvasing (id, kategori, nama_instansi, provinsi, kabupaten, kecamatan)
                    VALUES (nextval('canvasing_seq'), ?, ?, ?, ?, ?)
                """;

        pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                Canvasing c = list.get(i);
                ps.setString(1, c.getKategori());
                ps.setString(2, c.getNamaInstansi());
                ps.setString(3, c.getProvinsi());
                ps.setString(4, c.getKabupaten());
                ps.setString(5, c.getKecamatan());
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });

        list.clear(); // Kosongkan buffer
    }

    @Autowired
    private TkdnRepository tkdnRepository;

    // KONFIGURASI GOOGLE SHEET
    private static final String SPREADSHEET_ID = "173w5Y8hynv8lOphrsjtCx0tc8CJIQThuLrMIttwtw30";
    private static final String RANGE = "TKDN!A1:Z"; // Ambil dari A1 biar Header kebawa
    private static final String PRICELIST_RANGE = "PRICELIST&MODAL!A1:Z";
    private static final String CREDENTIALS_FILE_PATH = "/service_account.json";

    @Async
    public CompletableFuture<String> migrateTkdnData() {
        long startTime = System.currentTimeMillis();
        log.info("=== START MIGRASI TKDN (DYNAMIC HEADER MAPPING) ===");

        try {
            // 1. Koneksi Google Sheets
            InputStream in = MigrationService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) {
                return CompletableFuture.completedFuture("ERROR: service_account.json tidak ditemukan!");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));

            Sheets service = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Anandam Store")
                    .build();

            // 2. Ambil Data
            ValueRange response = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, RANGE)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return CompletableFuture.completedFuture("Data Kosong.");
            }

            // 3. Mapping Header (Baris Pertama)
            // Ini kuncinya! Kita cari tahu kolom "MODAL" itu ada di index ke berapa.
            List<Object> headerRow = values.get(0);
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headerRow.size(); i++) {
                // Simpan nama kolom dengan Huruf Besar semua biar aman
                headerMap.put(headerRow.get(i).toString().trim().toUpperCase(), i);
            }

            log.info("Header terdeteksi: {}", headerMap.keySet());

            // 4. Bersihkan Tabel
            log.info("Truncate tabel...");
            tkdnRepository.truncateTable();

            // 5. Proses Data (Mulai dari baris ke-2 / index 1)
            List<Tkdn> buffer = new ArrayList<>();
            int totalProcessed = 0;

            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.isEmpty())
                    continue;

                Tkdn t = new Tkdn();

                // Ambil data berdasarkan NAMA KOLOM (Sesuai Python script kamu)
                t.setKategori(getVal(row, headerMap, "KATEGORI"));
                t.setModal(cleanNumber(getVal(row, headerMap, "MODAL")));
                t.setDealer(getVal(row, headerMap, "DEALER")); // Di DB Varchar
                t.setPrincipal(getVal(row, headerMap, "PRINCIPLE")); // Perhatikan ejaan di sheet mungkin "PRINCIPLE"
                                                                     // atau "PRINCIPAL"

                // Cek typo kolom di sheet, kadang PRINCIPLE kadang PRINCIPAL
                if (t.getPrincipal() == null) {
                    t.setPrincipal(getVal(row, headerMap, "PRINCIPAL"));
                }

                t.setTayang(parseDate(getVal(row, headerMap, "TAYANG")));
                t.setSertifikatTkd(getVal(row, headerMap, "SERTIFIKAT TKDN"));
                t.setPresentase(cleanPercentage(getVal(row, headerMap, "PERSENTASE")));

                t.setNoMerek(getVal(row, headerMap, "NO MEREK"));
                t.setNama(getVal(row, headerMap, "NAMA LENGKAP")); // Sesuai python: NAMA LENGKAP
                t.setSpesifikasi(getVal(row, headerMap, "SPESIFIKASI LENGKAP"));
                t.setDistri(getVal(row, headerMap, "DISTRI"));

                t.setProcessor(getVal(row, headerMap, "PROCESSOR"));
                t.setRam(getVal(row, headerMap, "RAM"));
                t.setSsd(getVal(row, headerMap, "SSD"));
                t.setHdd(getVal(row, headerMap, "HDD"));
                t.setVga(getVal(row, headerMap, "VGA"));
                t.setLayar(getVal(row, headerMap, "LAYAR"));
                t.setOs(getVal(row, headerMap, "OS"));
                t.setGaransi(getVal(row, headerMap, "GARANSI"));

                buffer.add(t);

                if (buffer.size() >= 1000) {
                    totalProcessed += buffer.size();
                    self.saveTkdnBatch(buffer);
                    log.info("Migrated: {}", totalProcessed);
                }
            }

            if (!buffer.isEmpty()) {
                totalProcessed += buffer.size();
                self.saveTkdnBatch(buffer);
            }

            long duration = System.currentTimeMillis() - startTime;
            return CompletableFuture
                    .completedFuture("=== DONE === Total: " + totalProcessed + ". Time: " + (duration / 1000) + "s");

        } catch (Exception e) {
            log.error("Error during TKDN migration: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture("ERROR: " + e.getMessage());
        }
    }

    // Helper untuk ambil data aman berdasarkan nama kolom
    private String getVal(List<Object> row, Map<String, Integer> map, String colName) {
        if (!map.containsKey(colName))
            return null; // Kolom tidak ditemukan di header
        int idx = map.get(colName);
        if (idx >= row.size())
            return null; // Baris ini datanya kurang panjang
        Object val = row.get(idx);
        return (val == null) ? null : val.toString().trim();
    }

    @Transactional
    public void saveTkdnBatch(List<Tkdn> list) {
        // HAPUS 'id' dari daftar kolom
        // HAPUS 'nextval(...)' dari values
        String sql = """
                    INSERT INTO tkdn (kategori, modal, dealer, principal, tayang, sertifikat_tkd, presentase,
                                    no_merek, nama, spesifikasi, distri, processor, ram, ssd, hdd, vga, layar, os, garansi)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                Tkdn t = list.get(i);

                // Index dimulai dari 1 (Kategori)
                ps.setString(1, t.getKategori());

                if (t.getModal() != null)
                    ps.setInt(2, t.getModal());
                else
                    ps.setNull(2, java.sql.Types.INTEGER);

                ps.setString(3, t.getDealer());
                ps.setString(4, t.getPrincipal());

                if (t.getTayang() != null)
                    ps.setDate(5, java.sql.Date.valueOf(t.getTayang()));
                else
                    ps.setNull(5, java.sql.Types.DATE);

                ps.setString(6, t.getSertifikatTkd());
                ps.setBigDecimal(7, t.getPresentase());
                ps.setString(8, t.getNoMerek());
                ps.setString(9, t.getNama());
                ps.setString(10, t.getSpesifikasi());
                ps.setString(11, t.getDistri());
                ps.setString(12, t.getProcessor());
                ps.setString(13, t.getRam());
                ps.setString(14, t.getSsd());
                ps.setString(15, t.getHdd());
                ps.setString(16, t.getVga());
                ps.setString(17, t.getLayar());
                ps.setString(18, t.getOs());
                ps.setString(19, t.getGaransi());
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        list.clear();
    }

    // Logic Clean Number & Date sama seperti sebelumnya...
    private Integer cleanNumber(String val) {
        if (val == null || val.isEmpty())
            return 0;
        String s = val.replace("\u00A0", "").replace(" ", "").replace("Rp", "");
        // Logic ribuan Indonesia: 4.640.000 -> Hilangkan titik
        // Asumsi format Indonesia (titik = ribuan, koma = desimal)
        if (s.contains(".") && !s.contains(",")) {
            s = s.replace(".", "");
        } else if (s.contains(".") && s.contains(",")) {
            // 4.640.000,00 -> ambil depan koma, buang titik
            s = s.split(",")[0].replace(".", "");
        }
        s = s.replaceAll("[^0-9-]", "");
        try {
            return s.isEmpty() ? 0 : Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal cleanPercentage(String val) {
        if (val == null || val.isEmpty())
            return BigDecimal.ZERO;
        // 36.77 -> Sudah pakai titik, aman untuk BigDecimal
        // 36,77 -> Ubah koma jadi titik
        String s = val.replace("%", "").replace(",", ".").trim();
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isEmpty())
            return null;
        try {
            return LocalDate.parse(val, DateTimeFormatter.ofPattern("d/M/yyyy"));
        } catch (Exception e) {
            try {
                return LocalDate.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
