package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Memo;
import com.stok.anandam.store.core.postgres.model.MemoLog;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import com.stok.anandam.store.core.postgres.repository.MemoLogRepository;
import com.stok.anandam.store.core.postgres.repository.MemoRepository;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class MemoSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(MemoSchedulerService.class);

    private final MemoRepository memoRepository;
    private final MemoLogRepository memoLogRepository;
    private final com.stok.anandam.store.core.postgres.repository.SalesRepository salesRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // Cached system user ID untuk menghindari query setiap 30 detik
    private Long systemUserId = null;

    private Long getSystemUserId() {
        if (systemUserId != null) return systemUserId;
        // Cari user dengan role ADMIN sebagai representasi system
        systemUserId = userRepository.findByUsername("admin")
                .map(User::getId)
                .orElseGet(() -> userRepository.findAll().stream()
                        .findFirst()
                        .map(User::getId)
                        .orElse(0L));
        return systemUserId;
    }

    /**
     * Auto-match JL setiap 30 detik untuk memo yang:
     * 1. Status MENUNGGU_GUDANG (sebelum Gudang selesai picking)
     * 2. Status MENUNGGU_NOTA (setelah Gudang selesai picking)
     *
     * Mencocokkan nama pelanggan + total harga dengan data Sales (MyBiz) di PostgreSQL.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoMatchJlScheduler() {
        try {
            Long aktorId = getSystemUserId();

            // Cari memo dengan status MENUNGGU_GUDANG atau MENUNGGU_NOTA yang belum punya nomor JL
            List<Memo> waitingMemos = memoRepository.findByStatusAkhirInAndNomorJlIsNullOrEmpty(
                    List.of(MemoStatus.MENUNGGU_GUDANG, MemoStatus.MENUNGGU_NOTA)
            );

            if (waitingMemos.isEmpty()) {
                return;
            }

            int matchCount = 0;
            for (Memo memo : waitingMemos) {
                boolean matched = tryAutoMatchJl(memo);
                if (matched) {
                    // Jika JL ditemukan, langsung lanjut ke next status (BUFFER_ZONE / MENUNGGU_TEKNISI)
                    // Tidak peduli apakah status saat ini MENUNGGU_GUDANG atau MENUNGGU_NOTA
                    MemoStatus targetStatus = Boolean.TRUE.equals(memo.getIsTeknisRequired())
                            ? MemoStatus.MENUNGGU_TEKNISI
                            : MemoStatus.BUFFER_ZONE;
                    memo.setStatusAkhir(targetStatus);
                    memoRepository.save(memo);
                    memoLogRepository.save(new MemoLog(
                            memo.getId(),
                            targetStatus.name(),
                            aktorId,
                            "JL otomatis ditemukan via scheduler: " + memo.getNomorJl()
                                    + ". Status langsung masuk " + targetStatus.name()
                    ));
                    matchCount++;
                }
            }

            if (matchCount > 0) {
                log.info("Auto-match JL scheduler: {} memo berhasil dicocokkan", matchCount);
                sendMemoRefreshSignal();
            }
        } catch (Exception e) {
            log.error("Error in autoMatchJlScheduler: {}", e.getMessage(), e);
        }
    }

    private boolean tryAutoMatchJl(Memo memo) {
        // Skip jika sudah punya nomor JL
        if (memo.getNomorJl() != null && !memo.getNomorJl().isBlank()) {
            return false;
        }

        // 1. Ambil nama pelanggan dari Memo
        String customerName = null;
        if (memo.getPelangganMybiz() != null) {
            customerName = memo.getPelangganMybiz().getNamaPartner();
        } else if (memo.getCustomer() != null) {
            customerName = memo.getCustomer().getNamaPelanggan();
        }

        if (customerName == null || customerName.isBlank()) return false;
        if (memo.getTotalHarga() == null || memo.getTotalHarga().compareTo(BigDecimal.ZERO) == 0) return false;

        // 2. Cari di Sales (sudah GROUP BY docNo ORDER BY MAX(docDate) DESC, jadi index 0 = terbaru)
        List<String> matches = salesRepository.findDocNoByParNameIgnoreCaseAndGrandTotal(
                customerName, memo.getTotalHarga());

        if (matches.isEmpty()) return false;

        // 3. Ambil yang terbaru (index 0)
        String bestMatchDocNo = matches.get(0);
        memo.setNomorJl(bestMatchDocNo);
        return true;
    }

    private void sendMemoRefreshSignal() {
        try {
            messagingTemplate.convertAndSend("/topic/memos", "REFRESH");
        } catch (Exception e) {
            log.warn("Gagal mengirim sinyal WebSocket: {}", e.getMessage());
        }
    }
}