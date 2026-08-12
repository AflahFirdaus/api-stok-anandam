package com.stok.anandam.store.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.stok.anandam.store.util.NormalizationUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit test untuk logika parsing sheet pricelist (fill-down + merge ke atas)
 * dan penanganan tanda hubung '-' pada nama item.
 *
 * Kolom diasumsikan: 0=NAMA BARANG, 1=SPESIFIKASI, 2=MODAL, 3=PRICELIST.
 * headerFound = true, headerRowIndex = 0.
 */
class MigrationServicePricelistParsingTest {

    private static List<Object> row(Object... vals) {
        return Arrays.asList(vals);
    }

    private static List<List<Object>> header() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("NAMA BARANG", "SPESIFIKASI", "MODAL FINAL", "PRICELIST"));
        return rows;
    }

    private static Map<String, Object[]> parse(List<List<Object>> values) {
        return MigrationService.buildPricelistDataMap(
                values, 0 /* headerRowIndex */, 0 /* idxItemName */, 0 /* idxItemCode */,
                1 /* idxSpesifikasi */, 2 /* idxModal */, 3 /* idxPricelist */, true /* headerFound */);
    }

    @Test
    void normalizeItemNameMenghilangkanTandaHubung() {
        assertThat(NormalizationUtil.normalizeItemName("NB ACER AG14-72P-58AK"))
                .isEqualTo("NB ACER AG14 72P 58AK");
        assertThat(NormalizationUtil.normalizeItemName("NB ACER AG14 72P 58AK"))
                .isEqualTo("NB ACER AG14 72P 58AK");
    }

    @Test
    void hyphenVersionDanTanpaHyphenDianggapSama() {
        // Dua baris: satu ditulis dgn '-', satu tanpa '-' -> harus terkumpul di key yang sama.
        List<List<Object>> values = header();
        values.add(row("NB ACER AG14-72P-58AK", "SPEC 1", "1000", null));
        values.add(row("NB ACER AG14 72P 58AK", null, null, "2000"));

        Map<String, Object[]> map = parse(values);
        assertThat(map).containsOnlyKeys("NB ACER AG14 72P 58AK");
        Object[] payload = map.get("NB ACER AG14 72P 58AK");
        assertThat(payload[1]).isEqualTo(new BigDecimal("1000"));
        assertThat(payload[2]).isEqualTo(new BigDecimal("2000"));
    }

    @Test
    void fillDownMenggabungkanBarisKosongKeProdukSebelumnya() {
        // Produk pada 2 baris: baris pertama nama, baris kedua (nama kosong / merged) berisi spec & pricelist.
        List<List<Object>> values = header();
        values.add(row("NB ACER AG14-72P-58AK", null, "1500000", null));
        values.add(row(null, "RAM 8GB / TFT 14", null, "1800000"));

        Map<String, Object[]> map = parse(values);
        assertThat(map).containsOnlyKeys("NB ACER AG14 72P 58AK");

        Object[] payload = map.get("NB ACER AG14 72P 58AK");
        assertThat(payload[0]).isEqualTo("RAM 8GB / TFT 14"); // spec dari baris lanjutan TIDAK hilang
        assertThat(payload[1]).isEqualTo(new BigDecimal("1500000"));
        assertThat(payload[2]).isEqualTo(new BigDecimal("1800000"));
        assertThat(payload[3]).isEqualTo("NB ACER AG14-72P-58AK"); // nama asli (dgn '-') disimpan
    }

    @Test
    void mergeKeAtasAmbilNilaiNonNullPertama() {
        // Nama sama di dua baris; baris kedua hanya isi pricelist (spec/modal dari baris pertama).
        List<List<Object>> values = header();
        values.add(row("ITEM A", "SPEC A", "1000", null));
        values.add(row("ITEM A", null, null, "2000"));

        Map<String, Object[]> map = parse(values);
        assertThat(map).containsOnlyKeys("ITEM A");

        Object[] payload = map.get("ITEM A");
        assertThat(payload[0]).isEqualTo("SPEC A");
        assertThat(payload[1]).isEqualTo(new BigDecimal("1000"));
        assertThat(payload[2]).isEqualTo(new BigDecimal("2000"));
    }
}