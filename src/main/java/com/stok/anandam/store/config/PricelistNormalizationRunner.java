package com.stok.anandam.store.config;

import com.stok.anandam.store.core.postgres.model.Pricelist;
import com.stok.anandam.store.core.postgres.repository.PricelistRepository;
import com.stok.anandam.store.util.NormalizationUtil;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Saat aplikasi start, pastikan semua baris pricelist yang sudah ada sebelumnya punya
 * normalized_item_name. Ini penting karena join stok &harr; pricelist sekarang memakai
 * kolom tersebut (tetap robust walau item_name lama disimpan dgn/tanpa tanda hubung).
 *
 * Idempotent: hanya memproses baris yang normalized_item_name-nya masih null/empty,
 * sehingga setelah berjalan sekali, startup berikutnya tidak melakukan apa-apa.
 */
@Component
public class PricelistNormalizationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PricelistNormalizationRunner.class);

    private final PricelistRepository pricelistRepository;

    public PricelistNormalizationRunner(PricelistRepository pricelistRepository) {
        this.pricelistRepository = pricelistRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Pricelist> missing = pricelistRepository.findByNormalizedItemNameIsNull();
            if (missing == null || missing.isEmpty()) {
                log.debug("Pricelist normalized_item_name: tidak ada baris yang perlu di-backfill.");
                return;
            }

            int n = 0;
            for (Pricelist p : missing) {
                if (p.getItemName() != null && !p.getItemName().isBlank()) {
                    p.setNormalizedItemName(NormalizationUtil.normalizeItemName(p.getItemName()));
                    n++;
                }
            }

            if (n > 0) {
                pricelistRepository.saveAll(missing);
                log.info("Pricelist normalized_item_name backfill (startup): {} rows diproses.", n);
            }
        } catch (Exception e) {
            // Jangan sampai menghalangi startup; log & lanjut.
            log.warn("Gagal backfill normalized_item_name pricelist saat startup: {}", e.getMessage());
        }
    }
}