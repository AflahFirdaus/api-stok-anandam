package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Memo;
import com.stok.anandam.store.core.postgres.model.PenjadwalanKonfirmasi;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import com.stok.anandam.store.core.postgres.model.enums.StatusJadwal;
import com.stok.anandam.store.core.postgres.model.enums.TipeTugas;
import com.stok.anandam.store.core.postgres.repository.MemoRepository;
import com.stok.anandam.store.core.postgres.repository.PenjadwalanKonfirmasiRepository;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.DeliveryScanResponse;
import com.stok.anandam.store.dto.WebResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryAssignmentService {

    private final MemoRepository memoRepository;
    private final PenjadwalanKonfirmasiRepository penjadwalanRepo;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private void sendMemoRefreshSignal() {
        try {
            messagingTemplate.convertAndSend("/topic/memos", "REFRESH");
        } catch (Exception e) {
            System.err.println("Gagal mengirim sinyal WebSocket: " + e.getMessage());
        }
    }

    /**
     * Delivery melakukan scan QR Code pada memo.
     * Sistem akan mencari memo berdasarkan QR Code yang di-scan,
     * kemudian membuat/memperbarui jadwal pengiriman dan menugaskan
     * delivery tersebut sebagai pengirim.
     */
    @Transactional
    public WebResponse<DeliveryScanResponse> scanAndAssign(String qrCode, String username) {
        User delivery = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        // Validasi role harus DELIVERY
        if (delivery.getRole() != com.stok.anandam.store.core.postgres.model.Role.DELIVERY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya role DELIVERY yang dapat melakukan scan QR");
        }

        // Cari memo berdasarkan QR Code
        Memo memo = memoRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Memo dengan QR Code tersebut tidak ditemukan"));

        // Validasi status memo - hanya yang sudah siap kirim yang bisa di-scan
        if (memo.getStatusAkhir() != MemoStatus.MENUNGGU_PENGIRIMAN
                && memo.getStatusAkhir() != MemoStatus.BUFFER_ZONE
                && memo.getStatusAkhir() != MemoStatus.DIJADWALKAN
                && memo.getStatusAkhir() != MemoStatus.MENUNGGU_EXPEDISI) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Memo dengan status " + memo.getStatusAkhir() + " tidak dapat di-scan untuk pengiriman. " +
                    "Status yang diizinkan: MENUNGGU_PENGIRIMAN, BUFFER_ZONE, DIJADWALKAN, MENUNGGU_EXPEDISI");
        }


        // Cari penjadwalan yang sudah ada untuk memo ini dengan tipe PENGIRIMAN
        List<PenjadwalanKonfirmasi> existingList = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memo.getId())
                .stream()
                .filter(p -> p.getTipeTugas() == TipeTugas.PENGIRIMAN)
                .collect(Collectors.toList());

        PenjadwalanKonfirmasi jadwal;
        if (!existingList.isEmpty()) {
            // Update jadwal yang sudah ada
            jadwal = existingList.get(0);
            jadwal.setPersonelId(delivery.getId());
            jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);
        } else {
            // Buat jadwal baru
            jadwal = PenjadwalanKonfirmasi.builder()
                    .memo(memo)
                    .tipeTugas(TipeTugas.PENGIRIMAN)
                    .personelId(delivery.getId())
                    .statusJadwal(StatusJadwal.DIJADWALKAN)
                    .alamatLengkap(memo.getCustomer() != null ?
                            memo.getCustomer().getAlamatDefault() : null)
                    .isUrgen(false)
                    .build();
        }

        penjadwalanRepo.save(jadwal);

        // Update status memo menjadi DIJADWALKAN jika masih MENUNGGU_PENGIRIMAN
        if (memo.getStatusAkhir() != MemoStatus.DIJADWALKAN) {
            memo.setStatusAkhir(MemoStatus.DIJADWALKAN);
            memoRepository.save(memo);
        }

        sendMemoRefreshSignal();

        // Siapkan response
        DeliveryScanResponse response = DeliveryScanResponse.builder()
                .memoId(memo.getId())
                .nomorMemo(memo.getNomorMemo())
                .penjadwalanId(jadwal.getId())
                .alamatLengkap(jadwal.getAlamatLengkap())
                .alamatMaps(jadwal.getAlamatMaps())
                .namaPenerima(memo.getCustomer() != null ? memo.getCustomer().getNamaPelanggan() : null)
                .noHpPenerima(memo.getCustomer() != null ? memo.getCustomer().getNoHp() : null)
                .pesan("Berhasil! Anda ditugaskan mengirimkan memo " + memo.getNomorMemo())
                .build();

        return WebResponse.<DeliveryScanResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Scan berhasil, Anda ditugaskan untuk mengirimkan barang")
                .data(response)
                .build();
    }


    /**
     * Delivery melepas tugas pengiriman (unassign).
     * Digunakan ketika delivery scan barang tetapi tidak jadi dikirim.
     */
    @Transactional
    public WebResponse<String> releaseTask(Long penjadwalanId, String username) {
        User delivery = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        // Validasi role harus DELIVERY
        if (delivery.getRole() != com.stok.anandam.store.core.postgres.model.Role.DELIVERY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya role DELIVERY yang dapat melepas tugas");
        }

        PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(penjadwalanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Penjadwalan tidak ditemukan"));

        // Validasi bahwa delivery ini benar-benar yang ditugaskan
        if (jadwal.getPersonelId() == null || !jadwal.getPersonelId().equals(delivery.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda tidak dapat melepas tugas yang bukan ditugaskan kepada Anda");
        }

        // Validasi status - hanya bisa release jika belum dalam pengiriman
        if (jadwal.getStatusJadwal() == StatusJadwal.SELESAI
                || jadwal.getStatusJadwal() == StatusJadwal.DALAM_PENGIRIMAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tidak dapat melepas tugas yang sudah " +
                    (jadwal.getStatusJadwal() == StatusJadwal.SELESAI ? "selesai" : "dalam pengiriman"));
        }

        // Hapus personelId (unassign)
        jadwal.setPersonelId(null);
        jadwal.setStatusJadwal(StatusJadwal.MENUNGGU_KONFIRMASI);
        penjadwalanRepo.save(jadwal);

        // Update status memo kembali ke MENUNGGU_PENGIRIMAN jika sebelumnya DIJADWALKAN
        if (jadwal.getMemo() != null) {
            Memo memo = jadwal.getMemo();
            if (memo.getStatusAkhir() == MemoStatus.DIJADWALKAN) {
                memo.setStatusAkhir(MemoStatus.MENUNGGU_PENGIRIMAN);
                memoRepository.save(memo);
            }
        }

        sendMemoRefreshSignal();

        return WebResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Berhasil melepas tugas pengiriman. Barang tidak jadi dikirim oleh Anda.")
                .data("OK")
                .build();
    }
}
