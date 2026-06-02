package com.stok.anandam.store.service;
import com.stok.anandam.store.core.postgres.model.*;
import com.stok.anandam.store.core.postgres.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.BadSqlGrammarException;
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
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);

    @Autowired
    @org.springframework.context.annotation.Lazy
    private MigrationService self;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;



    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PelangganMybizRepository pelangganMybizRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int BATCH_SIZE = 1000;

    private static final String SQL_PURCHASE = """
                SELECT
                    MAX(d.doc_date) as doc_date, d.doc_no, MAX(d.par_name) as par_name,
                    MAX(dept.code) AS dep_code, m.code AS item_code, MAX(m.name) AS item_name,
                    SUM(CASE WHEN d.doc_no LIKE '%%RB%%' THEN -t.qty_def ELSE t.qty_def END) AS qty_def,
                    MAX(t.price) as price,
                    SUM(CASE WHEN d.doc_no LIKE '%%RB%%' THEN -t.qty_def ELSE t.qty_def END * t.price) AS grand_total
                FROM dbtpurchasedoc d
                LEFT JOIN dbtpurchasetrans t ON d.id = t.doc_id
                LEFT JOIN dbmitem m ON t.ite_id = m.id
                LEFT JOIN dbmdepartment dept ON m.dep_id = dept.id
                GROUP BY d.doc_no, m.code
                ORDER BY doc_date DESC, MAX(d.id) DESC
            """;

    private String getSqlPurchase() {
        return SQL_PURCHASE;
    }

    @Async
    public CompletableFuture<String> migratePurchaseData() {
        return CompletableFuture.completedFuture("Migrasi Purchase telah dipindah ke project api-migration.");
    }


    // Helper: Reset Table (Dipisah biar Transaction-nya jelas)
    public void resetTable() {
        purchaseRepository.truncateTable();
    }

    @Transactional
    public void saveBatch(List<Purchase> purchases) {
        String sql = """
                    INSERT INTO purchases (id, doc_date, doc_no_p, par_name, dep_code, item_code, item_name, qty, price, grand_total, last_synced)
                    VALUES (nextval('purchase_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (doc_no_p, item_code) DO UPDATE SET
                        doc_date = EXCLUDED.doc_date,
                        par_name = EXCLUDED.par_name,
                        dep_code = EXCLUDED.dep_code,
                        item_name = EXCLUDED.item_name,
                        qty = EXCLUDED.qty,
                        price = EXCLUDED.price,
                        grand_total = EXCLUDED.grand_total,
                        last_synced = EXCLUDED.last_synced
                """;

        try {
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
                    ps.setTimestamp(10, java.sql.Timestamp.valueOf(p.getLastSynced()));
                }

                @Override
                public int getBatchSize() {
                    return purchases.size();
                }
            });
        } catch (BadSqlGrammarException e) {
            log.error("CRITICAL SQL ERROR in Purchase Migration (Schema Mismatch): {}", e.getSQLException().getMessage());
            log.warn("Lakukan eksekusi script fix_migration_schema.sql di database PostgreSQL segera.");
            throw e; // Biar transaksi rollback, tapi di level migrate, kita tangkap agar tidak loop.
        } finally {
            purchases.clear();
        }
    }

    @Autowired
    private SalesRepository salesRepository;

    private static final String SQL_SALES = """
                SELECT
                    MAX(d.doc_date) AS doc_date,
                    d.doc_no,
                    MAX(p.code) AS code,
                    MAX(dep.code) AS dep_code,
                    MAX(d.par_name) AS par_name,
                    t.ite_name,
                    SUM(
                        CASE
                            WHEN d.doc_no LIKE '%%RJ%%' THEN -t.qty_def
                            ELSE t.qty_def
                        END
                    ) AS qty_def,
                    MAX(t.price) AS price,
                    SUM(
                        (
                            CASE
                                WHEN d.doc_no LIKE '%%RJ%%' THEN -t.qty_def
                                ELSE t.qty_def
                            END * t.price
                        )
                    ) AS grand_total,
                    MAX(e.code) AS emp_code,
                    MAX(e.name) AS emp_name
                FROM dbtsalesdoc d
                LEFT JOIN dbtsalestrans t ON d.id = t.doc_id
                LEFT JOIN dbmemployee e ON d.emp_id = e.id
                LEFT JOIN dbmpartner p ON d.par_id = p.id
                LEFT JOIN dbmitem i ON t.ite_id = i.id
                LEFT JOIN dbmdepartment dep ON i.dep_id = dep.id
                GROUP BY 
                    d.doc_no, 
                    t.ite_name
                ORDER BY 
                    doc_date DESC, 
                    MAX(d.id) DESC
            """;

    private String getSqlSales() {
        return SQL_SALES;
    }

    @Async
    public CompletableFuture<String> migrateSalesData() {
        return CompletableFuture.completedFuture("Migrasi Sales telah dipindah ke project api-migration.");
    }

    @Transactional
    public void saveSalesBatch(List<Sales> salesList) {
        String sql = """
                    INSERT INTO sales (id, doc_date, doc_no, code, par_name, dep_code, item_name, qty, price, grand_total, emp_code, emp_name, last_synced)
                    VALUES (nextval('sales_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (doc_no, item_name) DO UPDATE SET
                        doc_date = EXCLUDED.doc_date,
                        code = EXCLUDED.code,
                        par_name = EXCLUDED.par_name,
                        dep_code = EXCLUDED.dep_code,
                        qty = EXCLUDED.qty,
                        price = EXCLUDED.price,
                        grand_total = EXCLUDED.grand_total,
                        emp_code = EXCLUDED.emp_code,
                        emp_name = EXCLUDED.emp_name,
                        last_synced = EXCLUDED.last_synced
                """;

        try {
            pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                    Sales s = salesList.get(i);
                    ps.setDate(1, java.sql.Date.valueOf(s.getDocDate()));
                    ps.setString(2, s.getDocNo());
                    ps.setString(3, s.getCode());
                    ps.setString(4, s.getParName());
                    ps.setString(5, s.getDepCode());
                    ps.setString(6, s.getItemName());
                    ps.setInt(7, s.getQty());
                    ps.setBigDecimal(8, s.getPrice());
                    ps.setBigDecimal(9, s.getGrandTotal());
                    ps.setString(10, s.getEmpCode());
                    ps.setString(11, s.getEmpName());
                    ps.setTimestamp(12, java.sql.Timestamp.valueOf(s.getLastSynced()));
                }

                @Override
                public int getBatchSize() {
                    return salesList.size();
                }
            });
        } catch (BadSqlGrammarException e) {
            log.error("CRITICAL SQL ERROR in Sales Migration (Schema Mismatch): {}", e.getSQLException().getMessage());
            log.warn("Pastikan UNIQUE CONSTRAINT (doc_no, item_name) tersedia di tabel sales.");
            throw e;
        } finally {
            salesList.clear();
        }
    }

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private SyncSettingsRepository syncSettingsRepository;

    @Autowired
    private PricelistRepository pricelistRepository;

    // Google Sheets Config
    private static final String CREDENTIALS_FILE_PATH = "/service_account.json";
    private static final String STOCK_SPREADSHEET_ID = "173w5Y8hynv8lOphrsjtCx0tc8CJIQThuLrMIttwtw30";
    private static final String STOCK_PRICELIST_RANGE = "'PRICELIST&MODAL'!A:Z";

    private static final String TKDN_SPREADSHEET_ID = "173w5Y8hynv8lOphrsjtCx0tc8CJIQThuLrMIttwtw30";
    private static final String TKDN_RANGE = "TKDN!A1:AE";

    public void checkAndTriggerMigration() {
        log.info("Triggering Google Sheets migration...");
        try {
            syncStockPricelistFromSheet().get();
            log.info("Auto-sync Google Sheets migration completed.");
        } catch (Exception e) {
            log.error("Error trigger check: {}", e.getMessage());
        }
    }

    private static final String SQL_STOCK = """
                SELECT
                    s.item_code,
                    s.item_name,
                    /* Field dari SQL lama tetap dipertahankan */
                    SUBSTRING_INDEX(s.item_code, ' ', 1) AS kategori_itemcode,
                    SUBSTRING_INDEX(s.item_name, ' ', 1) AS kategori_nama,
                    s.final_stock,
                    COALESCE(h.price_avg, 0) AS harga_hpp,
                    (s.final_stock * COALESCE(h.price_avg, 0)) AS grand_total,
                    s.warehouse_name
                FROM (
                    SELECT
                        combined.item_code,
                        combined.item_name,
                        SUM(combined.qty_movement) AS final_stock,
                        w.name AS warehouse_name
                    FROM (
                        -- PURCHASE
                        SELECT
                            d.war_id,
                            /* Logika penentuan item_code & name mengikuti SQL baru agar lebih aman (COALESCE) */
                            COALESCE(m.code, NULLIF(TRIM(t.ite_code),''), TRIM(t.ite_name)) AS item_code,
                            COALESCE(m.name, TRIM(t.ite_name)) AS item_name,
                            CASE UPPER(LEFT(TRIM(d.doc_no),2))
                                WHEN 'BL' THEN COALESCE(t.qty_def,0)
                                WHEN 'RB' THEN -COALESCE(t.qty_def,0)
                                WHEN 'KM' THEN COALESCE(t.qty_def,0)
                                WHEN 'KK' THEN COALESCE(t.qty_def,0)
                                ELSE 0
                            END AS qty_movement
                        FROM anandamid26.dbtpurchasedoc d
                        LEFT JOIN anandamid26.dbtpurchasetrans t ON d.id = t.doc_id
                        LEFT JOIN anandamid26.dbmitem m ON t.ite_id = m.id

                        UNION ALL

                        -- TRANSFER
                        SELECT
                            d.war_id,
                            COALESCE(m.code, NULLIF(TRIM(t.ite_code),''), TRIM(t.ite_name)) AS item_code,
                            COALESCE(m.name, TRIM(t.ite_name)) AS item_name,
                            CASE UPPER(LEFT(TRIM(d.doc_no),2))
                                WHEN 'II' THEN COALESCE(t.qty_def,0)
                                WHEN 'IO' THEN -COALESCE(t.qty_def,0)
                                WHEN 'KM' THEN COALESCE(t.qty_def,0)
                                WHEN 'KK' THEN COALESCE(t.qty_def,0)
                                ELSE 0
                            END AS qty_movement
                        FROM anandamid26.dbtitemtransferdoc d
                        LEFT JOIN anandamid26.dbtitemtransfertrans t ON d.id = t.doc_id
                        LEFT JOIN anandamid26.dbmitem m ON t.ite_id = m.id

                        UNION ALL

                        -- SALES
                        SELECT
                            d.war_id,
                            COALESCE(m.code, NULLIF(TRIM(t.ite_code),''), TRIM(t.ite_name)) AS item_code,
                            COALESCE(m.name, TRIM(t.ite_name)) AS item_name,
                            CASE UPPER(LEFT(TRIM(d.doc_no),2))
                                WHEN 'JL' THEN -COALESCE(t.qty_def,0)
                                WHEN 'RJ' THEN COALESCE(t.qty_def,0)
                                WHEN 'KM' THEN COALESCE(t.qty_def,0)
                                WHEN 'KK' THEN COALESCE(t.qty_def,0)
                                ELSE 0
                            END AS qty_movement
                        FROM anandamid26.dbtsalesdoc d
                        LEFT JOIN anandamid26.dbtsalestrans t ON d.id = t.doc_id
                        LEFT JOIN anandamid26.dbmitem m ON t.ite_id = m.id
                    ) AS combined
                    LEFT JOIN anandamid26.dbmwarehouse w ON combined.war_id = w.id
                    WHERE w.name IS NOT NULL
                    AND TRIM(w.name) <> ''
                    GROUP BY
                        w.id,
                        w.name,
                        combined.item_code,
                        combined.item_name
                    /* Menampilkan stok > 0 sesuai kriteria migrasi pada umumnya */
                    HAVING SUM(combined.qty_movement) > 0
                ) AS s

                LEFT JOIN (
                    SELECT
                        i.code AS item_code,
                        sa.price_avg
                    FROM anandamid26.dbtstockavg sa
                    JOIN anandamid26.dbmitem i ON sa.ite_id = i.id
                    JOIN (
                        SELECT
                            ite_id,
                            MAX(id) AS max_id
                        FROM anandamid26.dbtstockavg
                        GROUP BY ite_id
                    ) last_sa
                        ON sa.ite_id = last_sa.ite_id
                        AND sa.id = last_sa.max_id
                ) AS h
                    ON s.item_code = h.item_code

                ORDER BY s.item_name;
            """;

    private String getSqlStock() {
        return SQL_STOCK;
    }

    @Async
    public CompletableFuture<String> migrateStockData() {
        return CompletableFuture.completedFuture("Migrasi Stok telah dipindah ke project api-migration.");
    }

    /**
     * Baca sheet PRICELIST&MODAL, isi kolom kosong dengan nilai dari baris atas
     * (fill-down untuk spesifikasi & pricelist),
     * lalu update stok by item name.
     */
    @Async
    @Transactional
    public CompletableFuture<String> syncStockPricelistFromSheet() {
        try {
            InputStream in = MigrationService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null)
                return CompletableFuture.completedFuture("ERROR: service_account.json tidak ditemukan");

            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
            Sheets service = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Anandam Store")
                    .build();

            ValueRange response = service.spreadsheets().values().get(STOCK_SPREADSHEET_ID, STOCK_PRICELIST_RANGE)
                    .execute();
            List<List<Object>> values = response.getValues();
            if (values == null || values.size() < 2)
                return CompletableFuture.completedFuture("Sheet kosong atau hanya header");

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
                idxItemCode = 1; // fallback: asumsikan item code sama dengan item name jika tidak ada kolom
                                 // khusus
                idxModal = 2;
                idxPricelist = 3;
            }

            if (idxItemName == null) {
                return CompletableFuture
                        .completedFuture("ERROR: Kolom nama item tidak ditemukan (header/fallback gagal).");
            }

            // Fill-down: nilai kosong diisi dari baris atas (HANYA spesifikasi & pricelist)
            String lastSpesifikasi = null;
            BigDecimal lastPricelist = null;
            Map<String, Object[]> byItemName = new HashMap<>(); // normalized key -> [spesifikasi, modal,
                                                                // finalPricelist]
            List<Object[]> orderedSheetRows = new ArrayList<>(); // urutan asli sheet -> [itemName, spesifikasi, modal,
                                                                 // pricelist]

            for (int i = 0; i < values.size(); i++) {
                if (i == headerRowIndex)
                    continue; // skip baris header (jika ada)
                List<Object> row = values.get(i);
                String itemName = getValByIndex(row, idxItemName);
                String itemCode = getValByIndex(row, idxItemCode);
                String fallbackNameColA = (headerRowIndex < 0) ? getValByIndex(row, 0) : null;
                String spesifikasiRaw = getValByIndex(row, idxSpesifikasi);
                String modalStr = getValByIndex(row, idxModal);
                String pricelistStr = getValByIndex(row, idxPricelist);

                // Fill-down: kosong pakai nilai baris atas (spesifikasi & pricelist)
                if (spesifikasiRaw != null && !spesifikasiRaw.isBlank())
                    lastSpesifikasi = spesifikasiRaw;
                String spesifikasi = (spesifikasiRaw != null && !spesifikasiRaw.isBlank()) ? spesifikasiRaw
                        : lastSpesifikasi;

                // Modal TIDAK fill-down: jika kosong tetap null
                BigDecimal modal = (modalStr != null && !modalStr.isBlank()) ? cleanBigDecimal(modalStr) : null;

                if (pricelistStr != null && !pricelistStr.isBlank())
                    lastPricelist = cleanBigDecimal(pricelistStr);
                BigDecimal pricelist = (pricelistStr != null && !pricelistStr.isBlank()) ? cleanBigDecimal(pricelistStr)
                        : lastPricelist;

                Object[] payload = new Object[] { spesifikasi, modal, pricelist };
                if (itemName != null && !itemName.isBlank()) {
                    byItemName.put(com.stok.anandam.store.util.NormalizationUtil.normalizeItemName(itemName), payload);
                }
                if (itemCode != null && !itemCode.isBlank()) {
                    byItemName.put(com.stok.anandam.store.util.NormalizationUtil.normalizeItemName(itemCode), payload);
                }
                // Fallback penting untuk format tanpa header: kolom A kadang berisi nama item
                // versi panjang
                if (fallbackNameColA != null && !fallbackNameColA.isBlank()) {
                    byItemName.put(com.stok.anandam.store.util.NormalizationUtil.normalizeItemName(fallbackNameColA),
                            payload);
                }

                // Simpan urutan baris sheet untuk fallback by-order
                if (itemName != null && !itemName.isBlank()) {
                    orderedSheetRows.add(new Object[] { itemName, spesifikasi, modal, pricelist });
                }
            }

            List<Pricelist> pricelistUpdates = new ArrayList<>();
            int updated = 0;
            for (Map.Entry<String, Object[]> entry : byItemName.entrySet()) {
                String itemName = entry.getKey();
                Object[] payload = entry.getValue();

                Pricelist p = pricelistRepository.findByItemName(itemName)
                        .orElse(Pricelist.builder().itemName(itemName).build());

                p.setSpesifikasi((String) payload[0]);
                p.setModal((BigDecimal) payload[1]);
                p.setFinalPricelist((BigDecimal) payload[2]);

                pricelistUpdates.add(p);
                updated++;
            }

            pricelistRepository.saveAll(pricelistUpdates);
            log.info("Pricelist mapping keys total: {}", byItemName.size());
            return CompletableFuture.completedFuture("Pricelist sync: " + updated + " item di-update dari sheet.");
        } catch (Exception e) {
            log.error("Error sync pricelist from sheet: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture("ERROR sync: " + e.getMessage());
        }
    }

    // Normalization methods moved to NormalizationUtil

    private static Integer getHeaderIndexFirst(Map<String, Integer> headerMap, String... names) {
        for (String n : names) {
            if (headerMap.containsKey(n))
                return headerMap.get(n);
        }
        return null;
    }

    private static String getValByIndex(List<Object> row, Integer index) {
        if (index == null)
            return null;
        if (index >= row.size())
            return null;
        Object v = row.get(index);
        return (v == null) ? null : v.toString().trim();
    }

    private BigDecimal cleanBigDecimal(String val) {
        if (val == null || val.isBlank())
            return null;
        String s = val.replace("\u00A0", "").replace(" ", "").replace("Rp", "");
        if (s.contains(".") && !s.contains(","))
            s = s.replace(".", "");
        else if (s.contains(".") && s.contains(","))
            s = s.split(",")[0].replace(".", "");
        s = s.replaceAll("[^0-9.-]", "");
        if (s.isEmpty())
            return null;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void saveStockBatch(List<Stock> stockList) {
        String sql = """
                    INSERT INTO stok (id, item_code, item_name, kategori_nama, kategori_itemcode, final_stok, harga_hpp, grand_total, warehouse, last_synced, normalized_item_name)
                    VALUES (nextval('stok_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (item_code, warehouse) DO UPDATE SET
                        item_name = EXCLUDED.item_name,
                        kategori_nama = EXCLUDED.kategori_nama,
                        kategori_itemcode = EXCLUDED.kategori_itemcode,
                        final_stok = EXCLUDED.final_stok,
                        harga_hpp = EXCLUDED.harga_hpp,
                        grand_total = EXCLUDED.grand_total,
                        last_synced = EXCLUDED.last_synced,
                        normalized_item_name = EXCLUDED.normalized_item_name
                """;

        try {
            pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                    Stock s = stockList.get(i);
                    ps.setString(1, s.getItemCode());
                    ps.setString(2, s.getItemName());
                    ps.setString(3, s.getKategoriNama());
                    ps.setString(4, s.getKategoriItemcode());

                    if (s.getFinalStok() != null) {
                        ps.setInt(5, s.getFinalStok());
                    } else {
                        ps.setNull(5, java.sql.Types.INTEGER);
                    }

                    ps.setBigDecimal(6, s.getHargaHpp());
                    ps.setBigDecimal(7, s.getGrandTotal());
                    ps.setString(8, s.getWarehouse());
                    ps.setTimestamp(9, java.sql.Timestamp.valueOf(s.getLastSynced()));
                    ps.setString(10, s.getNormalizedItemName());
                }

                @Override
                public int getBatchSize() {
                    return stockList.size();
                }
            });
        } catch (BadSqlGrammarException e) {
            log.error("CRITICAL SQL ERROR in Stock Migration: {}", e.getSQLException().getMessage());
            log.warn("Pastikan UNIQUE CONSTRAINT (item_code, warehouse) tersedia di tabel stok.");
            throw e;
        } finally {
            stockList.clear();
        }
    }

    @Async
    public CompletableFuture<String> migrateSnData() {
        return CompletableFuture.completedFuture("Migrasi Item SN telah dipindah ke project api-migration.");
    }

    /** Log penyebab koneksi JDBC (root cause) agar mudah debug MySQL/Postgres. */
    private void logConnectionCause(Throwable e) {
        Throwable cause = e;
        int depth = 0;
        while (cause != null && depth < 10) {
            log.error("Caused by [{}]: {} - {}", depth, cause.getClass().getSimpleName(), cause.getMessage());
            if (cause instanceof java.sql.SQLException) {
                java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
                if (sqlEx.getSQLState() != null)
                    log.error("  SQLState: {}", sqlEx.getSQLState());
                if (sqlEx.getErrorCode() != 0)
                    log.error("  ErrorCode: {}", sqlEx.getErrorCode());
            }
            cause = cause.getCause();
            depth++;
        }
    }

    private void saveSnBatch(List<Object[]> data) {
        String sql = """
                    INSERT INTO item_serial_numbers (tanggal, doc_id, user_name, item_name, sn, type, last_synced)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (sn, doc_id, type) DO UPDATE SET
                        tanggal = EXCLUDED.tanggal,
                        user_name = EXCLUDED.user_name,
                        item_name = EXCLUDED.item_name,
                        last_synced = EXCLUDED.last_synced
                """;
        try {
            pgJdbcTemplate.batchUpdate(sql, data, new int[] {
                    java.sql.Types.TIMESTAMP, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR,
                    java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR,
                    java.sql.Types.TIMESTAMP
            });
        } catch (BadSqlGrammarException e) {
            log.error("CRITICAL SQL ERROR in SN Migration: {}", e.getSQLException().getMessage());
            log.warn("Pastikan UNIQUE CONSTRAINT (sn, doc_id, type) tersedia di tabel item_serial_numbers.");
            throw e;
        } finally {
            data.clear();
        }
    }

    @Autowired
    private CanvasingRepository canvasingRepository;

    @Async
    public CompletableFuture<String> migrateCanvasingData() {
        LocalDateTime syncTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();
        log.info("=== START MIGRASI CANVASING (UPSERT) ===");

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
                }

                String[] data = line.split(",");

                if (data.length >= 2) {
                    Canvasing c = new Canvasing();

                    c.setKategori(safeGet(data, 0));
                    c.setNamaInstansi(safeGet(data, 1));
                    c.setProvinsi(safeGet(data, 2));
                    c.setKabupaten(safeGet(data, 3));
                    c.setKecamatan(safeGet(data, 4));
                    c.setLastSynced(syncTime);

                    if (c.getNamaInstansi() != null && !c.getNamaInstansi().isEmpty()) {
                        buffer.add(c);
                    }
                }

                if (buffer.size() >= BATCH_SIZE) {
                    totalProcessed[0] += buffer.size();
                    self.saveCanvasingBatch(buffer);
                    log.info("Canvasing Migrated: {}...", totalProcessed[0]);
                }
            }
            reader.close();

            // Sisa Data
            if (!buffer.isEmpty()) {
                totalProcessed[0] += buffer.size();
                self.saveCanvasingBatch(buffer);
            }

            // CLEANUP DATA LAMA
            log.info("Cleaning up old Canvasing data...");
            int deleted = pgJdbcTemplate.update("DELETE FROM canvasing WHERE last_synced < ?", syncTime);
            log.info("Cleaned up {} stale Canvasing records.", deleted);

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
                    INSERT INTO canvasing (id, kategori, nama_instansi, provinsi, kabupaten, kecamatan, last_synced)
                    VALUES (nextval('canvasing_seq'), ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (nama_instansi, kategori) DO UPDATE SET
                        provinsi = EXCLUDED.provinsi,
                        kabupaten = EXCLUDED.kabupaten,
                        kecamatan = EXCLUDED.kecamatan,
                        last_synced = EXCLUDED.last_synced
                """;

        try {
            pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                    Canvasing c = list.get(i);
                    ps.setString(1, c.getKategori());
                    ps.setString(2, c.getNamaInstansi());
                    ps.setString(3, c.getProvinsi());
                    ps.setString(4, c.getKabupaten());
                    ps.setString(5, c.getKecamatan());
                    ps.setTimestamp(6, java.sql.Timestamp.valueOf(c.getLastSynced()));
                }

                @Override
                public int getBatchSize() {
                    return list.size();
                }
            });
        } catch (BadSqlGrammarException e) {
            log.error("CRITICAL SQL ERROR in Canvasing Migration: {}", e.getSQLException().getMessage());
            log.warn("Pastikan UNIQUE CONSTRAINT (nama_instansi, kategori) tersedia di tabel canvasing.");
            throw e;
        } finally {
            list.clear(); // Kosongkan buffer
        }
    }

    @Autowired
    private TkdnRepository tkdnRepository;

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
                    .get(TKDN_SPREADSHEET_ID, TKDN_RANGE)
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
                t.setDealer(cleanNumber(getVal(row, headerMap, "DEALER"))); // Di DB Varchar
                t.setPrincipal(cleanNumber(getVal(row, headerMap, "PRINCIPLE"))); // Perhatikan ejaan di sheet mungkin
                                                                                  // "PRINCIPLE"
                // atau "PRINCIPAL"

                // Cek typo kolom di sheet, kadang PRINCIPLE kadang PRINCIPAL
                if (t.getPrincipal() == null) {
                    t.setPrincipal(cleanNumber(getVal(row, headerMap, "PRINCIPAL")));
                }

                t.setTayang(cleanNumber(getVal(row, headerMap, "TAYANG")));
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

                if (t.getDealer() != null)
                    ps.setInt(3, t.getDealer());
                else
                    ps.setNull(3, java.sql.Types.INTEGER);

                if (t.getPrincipal() != null)
                    ps.setInt(4, t.getPrincipal());
                else
                    ps.setNull(4, java.sql.Types.INTEGER);

                if (t.getTayang() != null)
                    ps.setInt(5, t.getTayang());
                else
                    ps.setNull(5, java.sql.Types.INTEGER);

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

    private static final String SQL_PELANGGAN_MYBIZ = """
                SELECT 
                    p.code AS kode_partner,
                    p.name AS nama_partner,
                    p.emp_id AS mybiz_emp_id,       
                    e.code AS kode_marketing,             
                    e.name AS nama_marketing,             
                    p.ar_limit AS limit_piutang,
                    p.ar_term AS termin_piutang,
                    p.ap_limit AS limit_hutang,
                    p.ap_term AS termin_hutang,
                    p.npwp,
                    p.address AS alamat,
                    TRIM(SUBSTRING_INDEX(p.phone1, ' /', 1)) AS no_telepon
                FROM dbmpartner p
                INNER JOIN dbmemployee e ON p.emp_id = e.id
                WHERE UPPER(e.name) IN ('RYAN', 'AHMAD', 'ACHMAD', 'TEGAR', 'BACHTIAR')
                ORDER BY e.name ASC, p.code ASC
            """;

    private String getSqlPelangganMybiz() {
        return SQL_PELANGGAN_MYBIZ;
    }

    @Async
    public CompletableFuture<String> migratePelangganMybizData() {
        LocalDateTime syncTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        log.info("=== START MIGRASI PELANGGAN MYBIZ DARI GOOGLE SHEETS (UPSERT) ===");
        
        try {
            /* COMMENTED MYBIZ SOURCE FOR FUTURE USE:
            // 0. HITUNG ESTIMASI DATA
            String countSql = "SELECT COUNT(*) FROM (" + getSqlPelangganMybiz() + ") as total";
            try {
                Integer totalRows = legacyJdbcTemplate.queryForObject(countSql, Integer.class);
                log.info("ESTIMASI TOTAL DATA PELANGGAN MYBIZ DARI SOURCE: {}", totalRows);
            } catch (Exception e) {
                log.warn("Gagal menghitung total data source: {}", e.getMessage());
            }

            final List<PelangganMybiz> buffer = new ArrayList<>();
            final int[] totalProcessed = { 0 };

            // 1. Streaming Data
            legacyJdbcTemplate.query(getSqlPelangganMybiz(), new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    try {
                        PelangganMybiz pm = new PelangganMybiz();

                        pm.setKodePartner(rs.getString("kode_partner"));
                        pm.setNamaPartner(rs.getString("nama_partner"));
                        
                        long empIdVal = rs.getLong("mybiz_emp_id");
                        if (!rs.wasNull()) {
                            pm.setMybizEmpId(empIdVal);
                        }
                        
                        pm.setKodeMarketing(rs.getString("kode_marketing"));
                        pm.setNamaMarketing(rs.getString("nama_marketing"));
                        pm.setLimitPiutang(rs.getBigDecimal("limit_piutang"));
                        
                        int arTermVal = rs.getInt("termin_piutang");
                        if (!rs.wasNull()) {
                            pm.setTerminPiutang(arTermVal);
                        }
                        
                        pm.setLimitHutang(rs.getBigDecimal("limit_hutang"));
                        
                        int apTermVal = rs.getInt("termin_hutang");
                        if (!rs.wasNull()) {
                            pm.setTerminHutang(apTermVal);
                        }
                        
                        pm.setNpwp(rs.getString("npwp"));
                        pm.setAlamat(rs.getString("alamat"));
                        pm.setNoTelepon(rs.getString("no_telepon"));
                        pm.setLastSynced(syncTime);

                        buffer.add(pm);

                        if (buffer.size() >= BATCH_SIZE) {
                            totalProcessed[0] += buffer.size();
                            self.savePelangganMybizBatch(buffer);

                            log.info("PelangganMybiz Migrated: {}...", totalProcessed[0]);
                        }
                    } catch (BadSqlGrammarException e) {
                        throw e;
                    } catch (Exception e) {
                        log.warn("Error processing PelangganMybiz row: {}", e.getMessage());
                    }
                }
            });

            // Sisa Data
            if (!buffer.isEmpty()) {
                totalProcessed[0] += buffer.size();
                self.savePelangganMybizBatch(buffer);
            }
            */

            // === BARU: AMBIL DARI GOOGLE SPREADSHEET (SHEET: DISTRI DATA) ===
            InputStream in = MigrationService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null)
                return CompletableFuture.completedFuture("ERROR: service_account.json tidak ditemukan");

            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
            Sheets service = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Anandam Store")
                    .build();

            ValueRange response = service.spreadsheets().values().get(STOCK_SPREADSHEET_ID, "'DISTRI DATA'!A:H")
                    .execute();
            List<List<Object>> values = response.getValues();
            if (values == null || values.size() < 2)
                return CompletableFuture.completedFuture("Sheet DISTRI DATA kosong atau hanya header");

            // Header scanning
            int headerRowIndex = -1;
            Map<String, Integer> headerMap = new HashMap<>();
            for (int r = 0; r < values.size(); r++) {
                List<Object> possibleHeader = values.get(r);
                Map<String, Integer> probe = new HashMap<>();
                for (int c = 0; c < possibleHeader.size(); c++) {
                    probe.put(possibleHeader.get(c).toString().trim().toUpperCase(), c);
                }
                Integer probeCode = getHeaderIndexFirst(probe, "CODE PELANGGAN", "KODE PELANGGAN", "CODE", "KODE PARTNER", "KODE");
                Integer probeName = getHeaderIndexFirst(probe, "NAMA PELANGGAN", "NAMA PARTNER", "NAMA");
                if (probeCode != null && probeName != null) {
                    headerRowIndex = r;
                    headerMap = probe;
                    break;
                }
            }

            Integer idxCode = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "CODE PELANGGAN", "KODE PELANGGAN", "CODE", "KODE PARTNER", "KODE") : 0;
            Integer idxName = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "NAMA PELANGGAN", "NAMA PARTNER", "NAMA") : 1;
            Integer idxSales = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "SALES", "MARKETING") : 2;
            Integer idxLimit = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "LIMIT PIUTANG", "LIMIT") : 3;
            Integer idxTermin = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "TERMIN PIUTA", "TERMIN PIUTANG", "TERMIN") : 4;
            Integer idxAlamat = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "ALAMAT") : 5;
            Integer idxTelp = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "TELP", "TELEPON", "NO TELEPON", "NO HP") : 6;
            Integer idxNpwp = headerRowIndex >= 0 ? getHeaderIndexFirst(headerMap, "NPWP") : 7;

            // Menggunakan LinkedHashMap untuk men-deduplikasi/mengamankan kode_partner ganda dari Google Sheet
            Map<String, PelangganMybiz> uniquePartners = new LinkedHashMap<>();
            List<String> duplicatesLog = new ArrayList<>();
            List<Integer> emptyRowsLog = new ArrayList<>();
            List<String> exactDupsLog = new ArrayList<>();

            int startRow = headerRowIndex >= 0 ? headerRowIndex + 1 : 1;
            for (int r = startRow; r < values.size(); r++) {
                List<Object> row = values.get(r);
                if (row.isEmpty() || row.size() <= (idxCode != null ? idxCode : 0)) {
                    emptyRowsLog.add(r + 1);
                    continue;
                }

                String kodePartner = getValByIndex(row, idxCode);
                if (kodePartner == null || kodePartner.trim().isEmpty()) {
                    emptyRowsLog.add(r + 1);
                    continue;
                }

                String namaPartner = getValByIndex(row, idxName);
                String sales = getValByIndex(row, idxSales);
                String limitStr = getValByIndex(row, idxLimit);
                String terminStr = getValByIndex(row, idxTermin);
                String alamat = getValByIndex(row, idxAlamat);
                String telp = getValByIndex(row, idxTelp);
                String npwp = getValByIndex(row, idxNpwp);

                // Parse limit piutang
                BigDecimal limitPiutang = BigDecimal.ZERO;
                if (limitStr != null && !limitStr.trim().isEmpty()) {
                    BigDecimal cleaned = cleanBigDecimal(limitStr);
                    if (cleaned != null) {
                        limitPiutang = cleaned;
                    }
                }

                // Parse termin piutang
                Integer terminPiutang = 1;
                if (terminStr != null && !terminStr.trim().isEmpty()) {
                    try {
                        terminPiutang = Integer.parseInt(terminStr.trim());
                    } catch (Exception e) {
                        log.warn("Gagal parsing termin piutang '{}' pada baris {}: {}", terminStr, r, e.getMessage());
                    }
                }

                // === PENANGANAN KODE PARTNER GANDA ===
                // Jika kode_partner sudah terpakai oleh pelanggan dengan nama berbeda,
                // tambahkan suffix unik secara otomatis agar kedua data tetap masuk
                String finalKode = kodePartner;
                if (uniquePartners.containsKey(finalKode)) {
                    PelangganMybiz existing = uniquePartners.get(finalKode);
                    if (!existing.getNamaPartner().equalsIgnoreCase(namaPartner)) {
                        int suffix = 2;
                        while (uniquePartners.containsKey(kodePartner + "_" + suffix)) {
                            suffix++;
                        }
                        finalKode = kodePartner + "_" + suffix;
                        duplicatesLog.add(String.format("Baris %d: Kode '%s' ganda. Diubah menjadi '%s' agar pelanggan '%s' dan '%s' tetap masuk bersamaan.", 
                            r + 1, kodePartner, finalKode, existing.getNamaPartner(), namaPartner));
                    } else {
                        // Duplikat persis (Kode & Nama sama), gabungkan/timpa saja
                        exactDupsLog.add(String.format("Baris %d: Duplikat Persis dari pelanggan '%s' (Kode: '%s')", 
                            r + 1, namaPartner, kodePartner));
                    }
                }

                PelangganMybiz pm = PelangganMybiz.builder()
                        .kodePartner(finalKode)
                        .namaPartner(namaPartner != null ? namaPartner : "Pelanggan Tanpa Nama")
                        .kodeMarketing(sales != null ? sales.toUpperCase() : "SYSTEM")
                        .namaMarketing(sales != null ? sales : "System")
                        .limitPiutang(limitPiutang)
                        .terminPiutang(terminPiutang)
                        .limitHutang(BigDecimal.ZERO)
                        .terminHutang(1)
                        .npwp(npwp)
                        .alamat(alamat)
                        .noTelepon(telp)
                        .lastSynced(syncTime)
                        .build();

                uniquePartners.put(finalKode, pm);
            }

            if (!duplicatesLog.isEmpty()) {
                log.warn("=== DETEKSI KODE PARTNER GANDA DI SPREADSHEET (TOTAL {} TEMUAN) ===", duplicatesLog.size());
                for (String dup : duplicatesLog.subList(0, Math.min(duplicatesLog.size(), 30))) {
                    log.warn("  {}", dup);
                }
            }

            if (!exactDupsLog.isEmpty()) {
                log.info("=== DETEKSI DUPLIKAT PERSIS DI SPREADSHEET (TOTAL {} DATA DIGABUNGKAN) ===", exactDupsLog.size());
                for (String dup : exactDupsLog.subList(0, Math.min(exactDupsLog.size(), 30))) {
                    log.info("  {}", dup);
                }
            }

            if (!emptyRowsLog.isEmpty()) {
                log.info("=== BARIS KOSONG DI SPREADSHEET (TOTAL {} BARIS DILEWATI) ===", emptyRowsLog.size());
                log.info("  Baris: {}", emptyRowsLog);
            }

            final List<PelangganMybiz> buffer = new ArrayList<>();
            int totalProcessed = 0;

            for (PelangganMybiz pm : uniquePartners.values()) {
                buffer.add(pm);

                if (buffer.size() >= BATCH_SIZE) {
                    totalProcessed += buffer.size();
                    self.savePelangganMybizBatch(buffer);
                    buffer.clear();
                    log.info("PelangganMybiz Google Sheets Migrated: {}...", totalProcessed);
                }
            }

            if (!buffer.isEmpty()) {
                totalProcessed += buffer.size();
                self.savePelangganMybizBatch(buffer);
                buffer.clear();
            }

            // 2. CLEANUP DATA LAMA
            log.info("Cleaning up old PelangganMybiz data...");
            int deleted = pgJdbcTemplate.update("DELETE FROM pelanggan_mybiz WHERE last_synced < ?", syncTime);
            log.info("Cleaned up {} stale PelangganMybiz records.", deleted);

            long duration = System.currentTimeMillis() - startTime;
            String result = "=== PELANGGAN GOOGLE SHEETS SELESAI === Total: " + totalProcessed + ". Waktu: " + (duration / 1000) + " detik.";
            log.info("{}", result);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("CRITICAL ERROR during PelangganMybiz Data Migration: {}", e.getMessage(), e);
            logConnectionCause(e);
            return CompletableFuture.completedFuture("ERROR: " + e.getMessage());
        }
    }

    @Transactional
    public void savePelangganMybizBatch(List<PelangganMybiz> list) {
        String sql = """
                    INSERT INTO pelanggan_mybiz (id, kode_partner, nama_partner, mybiz_emp_id, kode_marketing, nama_marketing, limit_piutang, termin_piutang, limit_hutang, termin_hutang, npwp, alamat, no_telepon, last_synced)
                    VALUES (nextval('pelanggan_mybiz_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (kode_partner) DO UPDATE SET
                        nama_partner = EXCLUDED.nama_partner,
                        mybiz_emp_id = EXCLUDED.mybiz_emp_id,
                        kode_marketing = EXCLUDED.kode_marketing,
                        nama_marketing = EXCLUDED.nama_marketing,
                        limit_piutang = EXCLUDED.limit_piutang,
                        termin_piutang = EXCLUDED.termin_piutang,
                        limit_hutang = EXCLUDED.limit_hutang,
                        termin_hutang = EXCLUDED.termin_hutang,
                        npwp = EXCLUDED.npwp,
                        alamat = EXCLUDED.alamat,
                        no_telepon = EXCLUDED.no_telepon,
                        last_synced = EXCLUDED.last_synced
                """;

        try {
            pgJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                    PelangganMybiz pm = list.get(i);
                    ps.setString(1, pm.getKodePartner());
                    ps.setString(2, pm.getNamaPartner());
                    
                    if (pm.getMybizEmpId() != null) {
                        ps.setLong(3, pm.getMybizEmpId());
                    } else {
                        ps.setNull(3, java.sql.Types.BIGINT);
                    }
                    
                    ps.setString(4, pm.getKodeMarketing());
                    ps.setString(5, pm.getNamaMarketing());
                    ps.setBigDecimal(6, pm.getLimitPiutang());
                    
                    if (pm.getTerminPiutang() != null) {
                        ps.setInt(7, pm.getTerminPiutang());
                    } else {
                        ps.setNull(7, java.sql.Types.INTEGER);
                    }
                    
                    ps.setBigDecimal(8, pm.getLimitHutang());
                    
                    if (pm.getTerminHutang() != null) {
                        ps.setInt(9, pm.getTerminHutang());
                    } else {
                        ps.setNull(9, java.sql.Types.INTEGER);
                    }
                    
                    ps.setString(10, pm.getNpwp());
                    ps.setString(11, pm.getAlamat());
                    ps.setString(12, pm.getNoTelepon());
                    ps.setTimestamp(13, java.sql.Timestamp.valueOf(pm.getLastSynced()));
                }

                @Override
                public int getBatchSize() {
                    return list.size();
                }
            });
        } catch (BadSqlGrammarException e) {
            log.error("CRITICAL SQL ERROR in PelangganMybiz Migration (Schema Mismatch): {}", e.getSQLException().getMessage());
            throw e;
        } finally {
            list.clear();
        }
    }
}
