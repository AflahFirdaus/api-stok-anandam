package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Memo;
import com.stok.anandam.store.core.postgres.model.PenjadwalanKonfirmasi;
import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import com.stok.anandam.store.core.postgres.model.enums.StatusJadwal;
import com.stok.anandam.store.core.postgres.model.enums.TipeTugas;
import com.stok.anandam.store.core.postgres.repository.MemoRepository;
import com.stok.anandam.store.core.postgres.repository.PenjadwalanKonfirmasiRepository;
import com.stok.anandam.store.core.postgres.model.MemoLog;
import com.stok.anandam.store.core.postgres.repository.MemoLogRepository;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.core.postgres.model.MemoItem;
import com.stok.anandam.store.core.postgres.model.RequestDelivery;
import com.stok.anandam.store.core.postgres.repository.MemoItemRepository;
import com.stok.anandam.store.core.postgres.repository.RequestDeliveryRepository;
import com.stok.anandam.store.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PenjadwalanService {

    private final PenjadwalanKonfirmasiRepository penjadwalanRepo;
    private final MemoRepository memoRepository;
    private final MemoLogRepository memoLogRepository;
    private final UserRepository userRepository;
    private final MemoItemRepository memoItemRepository;
    private final RequestDeliveryRepository requestDeliveryRepository;
    private final FileService fileService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public WebResponse<List<PenjadwalanResponse>> getListTugas(TipeTugas tipe, StatusJadwal status, String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        List<PenjadwalanKonfirmasi> listTugas;
        if (tipe != null && status != null) {
            if (status == StatusJadwal.MENUNGGU_KONFIRMASI) {
                listTugas = new java.util.ArrayList<>();
                listTugas.addAll(penjadwalanRepo.findByTipeTugasAndStatusJadwalOrderByCreatedAtDesc(tipe, StatusJadwal.MENUNGGU_KONFIRMASI));
                listTugas.addAll(penjadwalanRepo.findByTipeTugasAndStatusJadwalOrderByCreatedAtDesc(tipe, StatusJadwal.DIJADWALKAN));
            } else {
                listTugas = penjadwalanRepo.findByTipeTugasAndStatusJadwalOrderByCreatedAtDesc(tipe, status);
            }
        } else if (tipe != null) {
            listTugas = penjadwalanRepo.findAllByOrderByCreatedAtDesc().stream()
                    .filter(t -> t.getTipeTugas() == tipe)
                    .collect(Collectors.toList());
        } else if (status != null) {
            if (status == StatusJadwal.MENUNGGU_KONFIRMASI) {
                listTugas = new java.util.ArrayList<>();
                listTugas.addAll(penjadwalanRepo.findByStatusJadwalAndDeletedAtIsNullOrderByCreatedAtDesc(StatusJadwal.MENUNGGU_KONFIRMASI));
                listTugas.addAll(penjadwalanRepo.findByStatusJadwalAndDeletedAtIsNullOrderByCreatedAtDesc(StatusJadwal.DIJADWALKAN));
            } else {
                listTugas = penjadwalanRepo.findByStatusJadwalAndDeletedAtIsNullOrderByCreatedAtDesc(status);
            }
        } else {
            listTugas = penjadwalanRepo.findAllByOrderByCreatedAtDesc();
        }
        
        // Filter out deleted items (SQLRestriction is already there but findAll needs care if manual filter)
        listTugas = listTugas.stream().filter(t -> {
            try {
               // Checking if t has deletedAt is handled by Hibernate's @SQLRestriction
               return true; 
            } catch (Exception e) { return true; }
        }).collect(Collectors.toList());
        
        // ROLE-BASED FILTERING
        // Teknisi, Delivery & Marketing hanya dapat melihat tugas yang di-assign ke diri mereka sendiri
        String roleName = aktor.getRole().name();
        if (roleName.equals("DELIVERY")) {
            listTugas = listTugas.stream()
                .filter(t -> t.getPersonelId() != null && t.getPersonelId().equals(aktor.getId()))
                .collect(Collectors.toList());
        }
        // Technicians (TEKNISI) bypass this filter to access the shared task pool
        
        List<PenjadwalanResponse> responses = listTugas.stream().map(tugas -> {
            String noRef = "Tanpa Referensi";
            UUID memoId = null;
            Long rdId = null;
            String nomorRef = null;

            if (tugas.getMemo() != null) {
                memoId = tugas.getMemo().getId();
                nomorRef = tugas.getMemo().getNomorMemo();
                noRef = nomorRef;
            } else if (tugas.getRequestDelivery() != null) {
                rdId = tugas.getRequestDelivery().getId();
                nomorRef = tugas.getRequestDelivery().getNomorRequest();
                noRef = nomorRef;
            }

            return PenjadwalanResponse.builder()
                    .id(tugas.getId())
                    .memoId(memoId)
                    .nomorMemo(memoId != null ? noRef : null)
                    .requestDeliveryId(rdId)
                    .nomorRequest(rdId != null ? noRef : null)
                    .tipeTugas(tugas.getTipeTugas())
                    .statusJadwal(tugas.getStatusJadwal())
                    .alamatLengkap(tugas.getAlamatLengkap())
                    .alamatMaps(tugas.getAlamatMaps())
                    .idKodepos(tugas.getIdKodepos())
                    .estimasiWaktu(tugas.getEstimasiWaktu())
                    .catatan(tugas.getCatatan())
                    .namaPenerima(tugas.getNamaPenerima())
                    .fotoBukti(tugas.getFotoBukti())
                    .catatanOperasional(tugas.getCatatanOperasional())
                    .tanggalJadwal(tugas.getTanggalJadwal() != null ? tugas.getTanggalJadwal().format(DATE_FORMATTER) : null)
                    .personelId(tugas.getPersonelId())
                    .isUrgen(tugas.getIsUrgen())
                    .marketingName(tugas.getMarketingName())
                    .manualCustomerName(tugas.getRequestDelivery() != null ? tugas.getRequestDelivery().getReceiverName() : tugas.getManualCustomerName())
                    .manualNoHp(tugas.getRequestDelivery() != null ? tugas.getRequestDelivery().getReceiverPhone() : tugas.getManualNoHp())
                    .kodePos(tugas.getKodepos() != null ? tugas.getKodepos().getKodePos() : null)
                    .desaKelurahan(tugas.getKodepos() != null ? tugas.getKodepos().getDesaKelurahan() : null)
                    .kecamatan(tugas.getKodepos() != null ? tugas.getKodepos().getKecamatan() : null)
                    .kabupatenKota(tugas.getKodepos() != null ? tugas.getKodepos().getKabupatenKota() : null)
                    .isExpeditionOutlet(tugas.getIsExpeditionOutlet())
                    .manifestNomors(getManifestNomors(tugas))
                    .latitude(tugas.getLatitude())
                    .longitude(tugas.getLongitude())
                    .build();
        }).collect(Collectors.toList());

        return WebResponse.<List<PenjadwalanResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Berhasil mengambil data tugas")
                .data(responses)
                .build();
    }

    public WebResponse<PenjadwalanResponse> getTugasDetail(Long id, String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        PenjadwalanKonfirmasi tugas = penjadwalanRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tugas penjadwalan tidak ditemukan"));

        // ROLE-BASED FILTERING
        String roleName = aktor.getRole().name();
        if (roleName.equals("TEKNISI") || roleName.equals("DELIVERY")) {
            if (!aktor.getId().equals(tugas.getPersonelId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke tugas ini");
            }
        }

        String noRef = "Tanpa Referensi";
        UUID memoId = null;
        Long rdId = null;
        String nomorRef = null;

        if (tugas.getMemo() != null) {
            memoId = tugas.getMemo().getId();
            nomorRef = tugas.getMemo().getNomorMemo();
            noRef = nomorRef;
        } else if (tugas.getRequestDelivery() != null) {
            rdId = tugas.getRequestDelivery().getId();
            nomorRef = tugas.getRequestDelivery().getNomorRequest();
            noRef = nomorRef;
        }

        PenjadwalanResponse response = PenjadwalanResponse.builder()
                .id(tugas.getId())
                .memoId(memoId)
                .nomorMemo(memoId != null ? noRef : null)
                .requestDeliveryId(rdId)
                .nomorRequest(rdId != null ? noRef : null)
                .tipeTugas(tugas.getTipeTugas())
                .statusJadwal(tugas.getStatusJadwal())
                .alamatLengkap(tugas.getAlamatLengkap())
                .alamatMaps(tugas.getAlamatMaps())
                .idKodepos(tugas.getIdKodepos())
                .estimasiWaktu(tugas.getEstimasiWaktu())
                .catatan(tugas.getCatatan())
                .namaPenerima(tugas.getNamaPenerima())
                .fotoBukti(tugas.getFotoBukti())
                .catatanOperasional(tugas.getCatatanOperasional())
                .tanggalJadwal(tugas.getTanggalJadwal() != null ? tugas.getTanggalJadwal().format(DATE_FORMATTER) : null)
                .personelId(tugas.getPersonelId())
                .isUrgen(tugas.getIsUrgen())
                .marketingName(tugas.getMarketingName())
                .manualCustomerName(tugas.getRequestDelivery() != null ? tugas.getRequestDelivery().getReceiverName() : tugas.getManualCustomerName())
                .manualNoHp(tugas.getRequestDelivery() != null ? tugas.getRequestDelivery().getReceiverPhone() : tugas.getManualNoHp())
                .kodePos(tugas.getKodepos() != null ? tugas.getKodepos().getKodePos() : null)
                .desaKelurahan(tugas.getKodepos() != null ? tugas.getKodepos().getDesaKelurahan() : null)
                .kecamatan(tugas.getKodepos() != null ? tugas.getKodepos().getKecamatan() : null)
                .kabupatenKota(tugas.getKodepos() != null ? tugas.getKodepos().getKabupatenKota() : null)
                .isExpeditionOutlet(tugas.getIsExpeditionOutlet())
                .manifestNomors(getManifestNomors(tugas))
                .latitude(tugas.getLatitude())
                .longitude(tugas.getLongitude())
                .build();

        return WebResponse.<PenjadwalanResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Berhasil mengambil detail tugas")
                .data(response)
                .build();
    }

    @Transactional
    public WebResponse<String> mulaiTugas(Long id, String username) {
        PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jadwal tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        if (jadwal.getStatusJadwal() != StatusJadwal.DIJADWALKAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hanya tugas dengan status DIJADWALKAN yang bisa dimulai");
        }

        jadwal.setStatusJadwal(StatusJadwal.DALAM_PENGIRIMAN);
        penjadwalanRepo.save(jadwal);

        if (jadwal.getMemo() != null) {
            Memo memo = jadwal.getMemo();
            memo.setStatusAkhir(MemoStatus.DALAM_PENGIRIMAN);
            memoRepository.save(memo);

            memoLogRepository.save(new MemoLog(
                    memo.getId(),
                    MemoStatus.DALAM_PENGIRIMAN.name(),
                    aktor.getId(),
                    "Tugas " + jadwal.getTipeTugas() + " dimulai oleh User: " + aktor.getNama()
            ));
        } else if (jadwal.getRequestDelivery() != null) {
            RequestDelivery rd = jadwal.getRequestDelivery();
            rd.setStatus(com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.DALAM_PENGIRIMAN);
            requestDeliveryRepository.save(rd);
        }

        return WebResponse.<String>builder()
                .status(200)
                .message("Tugas berhasil dimulai. Selamat bertugas!")
                .data(jadwal.getId().toString())
                .build();
    }

    @Transactional
    public WebResponse<String> updatePenjadwalan(Long id, UpdatePenjadwalanRequest request, String username) {
        PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jadwal tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        StringBuilder logMsg = new StringBuilder();
        boolean isChanged = false;

        // 1. Update Tanggal
        if (request.getTanggalJadwal() != null && !request.getTanggalJadwal().isBlank()) {
            LocalDate newDate = LocalDate.parse(request.getTanggalJadwal(), DATE_FORMATTER);
            if (!newDate.equals(jadwal.getTanggalJadwal())) {
                logMsg.append("Perubahan tanggal: ").append(jadwal.getTanggalJadwal()).append(" -> ").append(newDate).append(". ");
                jadwal.setTanggalJadwal(newDate);
                isChanged = true;
            }
        }

        // 2. Update Personel (Assign/Reassign)
        if (request.getPersonelId() != null) {
            if (!request.getPersonelId().equals(jadwal.getPersonelId())) {
                if (jadwal.getPersonelId() == null) {
                    logMsg.append("Assign ke Personel ID: ").append(request.getPersonelId()).append(". ");
                } else {
                    logMsg.append("Re-assign: ").append(jadwal.getPersonelId()).append(" -> ").append(request.getPersonelId()).append(". ");
                    if (request.getAlasan() != null) {
                        logMsg.append("Alasan: ").append(request.getAlasan()).append(". ");
                    }
                }
                jadwal.setPersonelId(request.getPersonelId());
                isChanged = true;
            }
        }

        // 3. Update Estimasi Waktu
        if (request.getEstimasiWaktu() != null) {
            jadwal.setEstimasiWaktu(request.getEstimasiWaktu());
            isChanged = true;
        }

        // 4. Update Catatan
        if (request.getCatatan() != null) {
            jadwal.setCatatan(request.getCatatan());
            isChanged = true;
        }

        // 5. Update Latitude & Longitude
        if (request.getLatitude() != null) {
            jadwal.setLatitude(request.getLatitude());
            isChanged = true;
        }
        if (request.getLongitude() != null) {
            jadwal.setLongitude(request.getLongitude());
            isChanged = true;
        }

        if (isChanged) {
            jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);
            penjadwalanRepo.save(jadwal);

            if (jadwal.getMemo() != null) {
                memoLogRepository.save(new MemoLog(
                        jadwal.getMemo().getId(),
                        jadwal.getMemo().getStatusAkhir().name(),
                        aktor.getId(),
                        "Update Penjadwalan " + jadwal.getTipeTugas() + ": " + logMsg.toString()
                ));
            }
        }

        return WebResponse.<String>builder()
                .status(200)
                .message("Penjadwalan berhasil diperbarui")
                .data(jadwal.getId().toString())
                .build();
    }

    @Transactional
    public WebResponse<String> reassign(Long id, com.stok.anandam.store.dto.ReassignRequest request, String username) {
        PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jadwal tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        Long oldPersonelId = jadwal.getPersonelId();
        jadwal.setPersonelId(request.getPersonelId());
        jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);
        penjadwalanRepo.save(jadwal);

        if (jadwal.getMemo() != null) {
            String msg = "Reassign " + jadwal.getTipeTugas() + " dari Personel ID: " + oldPersonelId + 
                         " ke Personel ID: " + request.getPersonelId();
            if (request.getAlasan() != null && !request.getAlasan().isBlank()) {
                msg += ". Alasan: " + request.getAlasan();
            }
            
            memoLogRepository.save(new MemoLog(
                    jadwal.getMemo().getId(),
                    jadwal.getMemo().getStatusAkhir().name(),
                    aktor.getId(),
                    msg
            ));
        }

        return WebResponse.<String>builder()
                .status(200)
                .message("Personel berhasil diganti")
                .data(jadwal.getId().toString())
                .build();
    }

    @Transactional
    public WebResponse<String> selesaikanTugas(Long idJadwal, MultipartFile photo, String namaPenerima, String catatanOperasional, String username) {
        PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(idJadwal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jadwal tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        if (jadwal.getStatusJadwal() != StatusJadwal.DALAM_PENGIRIMAN && jadwal.getStatusJadwal() != StatusJadwal.DIJADWALKAN) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tugas harus berstatus DALAM_PENGIRIMAN atau DIJADWALKAN untuk diselesaikan");
        }

        // Mandatory PoD check (Logic side)
        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto bukti wajib diunggah");
        }
        if (namaPenerima == null || namaPenerima.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama penerima wajib diisi");
        }

        // Save photo
        try {
            String fileName = fileService.saveMemoPhoto(photo);
            jadwal.setFotoBukti(fileName);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menyimpan foto bukti: " + e.getMessage());
        }

        jadwal.setNamaPenerima(namaPenerima);
        jadwal.setCatatanOperasional(catatanOperasional);
        jadwal.setStatusJadwal(StatusJadwal.SELESAI);
        penjadwalanRepo.save(jadwal);

        if (jadwal.getMemo() != null) {
            Memo memo = memoRepository.findById(jadwal.getMemo().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

            MemoStatus targetStatus = memo.getStatusAkhir();
            if (jadwal.getTipeTugas() == TipeTugas.TEKNISI) {
                targetStatus = MemoStatus.BUFFER_ZONE; 
            } else if (jadwal.getTipeTugas() == TipeTugas.PENGIRIMAN) {
                targetStatus = MemoStatus.DITERIMA_USER;
            }

            memo.setStatusAkhir(targetStatus);
            memoRepository.save(memo);

            memo.setBuktiFoto(jadwal.getFotoBukti());
            memoRepository.save(memo);

            MemoLog log = new MemoLog(
                memo.getId(),
                targetStatus.name(),
                aktor.getId(),
                "Tugas " + jadwal.getTipeTugas() + " diselesaikan oleh User: " + aktor.getNama() + ". Penerima: " + namaPenerima
            );
            memoLogRepository.save(log);
        } else if (jadwal.getRequestDelivery() != null) {
            RequestDelivery rd = jadwal.getRequestDelivery();
            rd.setStatus(com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.SELESAI);
            requestDeliveryRepository.save(rd);
        }

        return WebResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Tugas berhasil diselesaikan")
                .data(jadwal.getId().toString())
                .build();
    }

    @Transactional
    public WebResponse<String> createManualRequest(com.stok.anandam.store.dto.CreateManualJadwalRequest request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        // Create RequestDelivery FIRST
        RequestDelivery rd = RequestDelivery.builder()
                .nomorRequest(generateNomorRequest())
                .receiverName(request.getUserName())
                .receiverPhone(request.getNoHp())
                .alamatLengkap(request.getAlamatLengkap())
                .alamatMaps(request.getAlamatMaps())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .keterangan(request.getCatatan())
                .status(com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.MENUNGGU_GUDANG)
                .creator(creator)
                .build();
        
        rd = requestDeliveryRepository.save(rd);

        // Create PenjadwalanKonfirmasi for logistics tracking
        PenjadwalanKonfirmasi jadwal = new PenjadwalanKonfirmasi();
        jadwal.setRequestDelivery(rd);
        jadwal.setTipeTugas(request.getTipeTugas());
        jadwal.setStatusJadwal(StatusJadwal.MENUNGGU_KONFIRMASI);
        jadwal.setAlamatLengkap(request.getAlamatLengkap());
        jadwal.setAlamatMaps(request.getAlamatMaps());
        jadwal.setLatitude(request.getLatitude());
        jadwal.setLongitude(request.getLongitude());
        jadwal.setIdKodepos(request.getIdKodepos());
        jadwal.setCatatan(request.getCatatan());
        jadwal.setEstimasiWaktu(request.getEstimasiWaktu());
        jadwal.setIsUrgen(request.getIsUrgen() != null && request.getIsUrgen());
        jadwal.setMarketingName(creator.getNama());

        if (request.getTanggalJadwal() != null && !request.getTanggalJadwal().isBlank()) {
            jadwal.setTanggalJadwal(LocalDate.parse(request.getTanggalJadwal(), DATE_FORMATTER));
        }

        penjadwalanRepo.save(jadwal);

        return WebResponse.<String>builder()
                .status(HttpStatus.CREATED.value())
                .message("Request manual " + request.getTipeTugas() + " berhasil dibuat dengan nomor " + rd.getNomorRequest())
                .data(jadwal.getId().toString())
                .build();
    }

    private String generateNomorRequest() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = "REQ-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        long count = requestDeliveryRepository.findTopByOrderByCreatedAtDesc()
                .map(r -> {
                    if (r.getNomorRequest() != null && r.getNomorRequest().startsWith(prefix)) {
                        String suffix = r.getNomorRequest().substring(prefix.length() + 1);
                        try {
                            return Long.parseLong(suffix) + 1;
                        } catch (Exception e) { return 1L; }
                    }
                    return 1L;
                })
                .orElse(1L);

        return String.format("%s-%04d", prefix, count);
    }

    @Transactional
    public WebResponse<String> createPengambilan(com.stok.anandam.store.dto.CreatePengambilanRequest request, String username) {
        PenjadwalanKonfirmasi jadwal = new PenjadwalanKonfirmasi();

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));
        
        if (request.getMemoId() != null) {
            Memo memo = memoRepository.findById(request.getMemoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
            jadwal.setMemo(memo);
        }

        jadwal.setTipeTugas(TipeTugas.PENGAMBILAN);
        jadwal.setStatusJadwal(StatusJadwal.MENUNGGU_KONFIRMASI);
        jadwal.setAlamatLengkap(request.getAlamatLengkap());
        jadwal.setAlamatMaps(request.getAlamatMaps());
        jadwal.setIdKodepos(request.getIdKodepos());
        jadwal.setCatatan(request.getCatatan());
        jadwal.setEstimasiWaktu(request.getEstimasiWaktu());
        
        if (request.getTanggalJadwal() != null && !request.getTanggalJadwal().isBlank()) {
            jadwal.setTanggalJadwal(LocalDate.parse(request.getTanggalJadwal(), DATE_FORMATTER));
        }

        penjadwalanRepo.save(jadwal);

        if (jadwal.getMemo() != null) {
            MemoLog log = new MemoLog(
                jadwal.getMemo().getId(),
                jadwal.getMemo().getStatusAkhir().name(),
                aktor.getId(),
                "Request pengambilan barang dibuat oleh User ID: " + aktor.getId()
            );
            memoLogRepository.save(log);
        }

        return WebResponse.<String>builder()
                .status(HttpStatus.CREATED.value())
                .message("Request pengambilan berhasil dibuat, menunggu konfirmasi gudang")
                .data(jadwal.getId().toString())
                .build();
    }

    @Transactional
    public WebResponse<String> konfirmasiKirimJadwal(Long idJadwal, com.stok.anandam.store.dto.KonfirmasiKirimRequest request, String username) {
        PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(idJadwal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jadwal tidak ditemukan"));

        if (jadwal.getMemo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jadwal ini tidak terkait dengan memo apapun");
        }

        UUID memoId = jadwal.getMemo().getId();
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data item kirim tidak boleh kosong");
        }

        for (com.stok.anandam.store.dto.ItemKirim itemInput : request.getItems()) {
            MemoItem item = memoItemRepository.findById(itemInput.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item dengan ID " + itemInput.getItemId() + " tidak ditemukan"));

            if (!item.getMemo().getId().equals(memoId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item " + item.getNamaBarang() + " tidak termasuk dalam memo ini");
            }

            int qtyBaru = item.getQtyShipped() + itemInput.getQtyDikirimSaatIni();
            if (qtyBaru > item.getQty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kuantitas kirim melebihi batas pesanan untuk item: " + item.getNamaBarang());
            }

            item.setQtyShipped(qtyBaru);
            memoItemRepository.save(item);
        }

        List<MemoItem> allItems = memoItemRepository.findByMemoId(memoId);
        boolean isSemuaTerkirim = true;
        for (MemoItem item : allItems) {
            if (item.getQtyShipped() < item.getQty()) {
                isSemuaTerkirim = false;
                break;
            }
        }

        MemoStatus targetStatus = isSemuaTerkirim ? MemoStatus.DITERIMA_USER : MemoStatus.TERKIRIM_SEBAGIAN;
        Memo memo = jadwal.getMemo();
        memo.setStatusAkhir(targetStatus);
        memoRepository.save(memo);

        if (jadwal.getTipeTugas() == TipeTugas.PENGIRIMAN) {
            jadwal.setStatusJadwal(StatusJadwal.SELESAI);
            penjadwalanRepo.save(jadwal);
        }

        MemoLog log = new MemoLog(
                memo.getId(),
                targetStatus.name(),
                aktor.getId(),
                "Konfirmasi pengiriman via Jadwal ID: " + idJadwal + " (" + (isSemuaTerkirim ? "Lengkap" : "Sebagian") + ") oleh User ID: " + aktor.getId()
        );
        memoLogRepository.save(log);

        return WebResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Konfirmasi pengiriman via Jadwal berhasil disimpan. Status Memo: " + targetStatus)
                .data(jadwal.getId().toString())
                .build();
    }

    private List<String> getManifestNomors(PenjadwalanKonfirmasi tugas) {
        List<String> nomors = new ArrayList<>();
        if (tugas.getManifestMemos() != null) {
            tugas.getManifestMemos().forEach(m -> nomors.add(m.getNomorMemo()));
        }
        if (tugas.getManifestRequests() != null) {
            tugas.getManifestRequests().forEach(r -> nomors.add(r.getNomorRequest()));
        }
        // Fallback to single refs if manifest is empty
        if (nomors.isEmpty()) {
            if (tugas.getMemo() != null) nomors.add(tugas.getMemo().getNomorMemo());
            else if (tugas.getRequestDelivery() != null) nomors.add(tugas.getRequestDelivery().getNomorRequest());
        }
        return nomors;
    }

    @Transactional
    public WebResponse<String> createBulkPenjadwalan(BulkPenjadwalanRequest request, String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        int count = 0;

        if (request.getRequestDeliveryIds() != null) {
            for (Long rdId : request.getRequestDeliveryIds()) {
                RequestDelivery rd = requestDeliveryRepository.findById(rdId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request Delivery ID " + rdId + " tidak ditemukan"));

                // Try to find existing
                List<PenjadwalanKonfirmasi> existingList = penjadwalanRepo.findAllByOrderByCreatedAtDesc().stream()
                        .filter(p -> p.getRequestDelivery() != null && p.getRequestDelivery().getId().equals(rdId))
                        .collect(Collectors.toList());

                PenjadwalanKonfirmasi jadwal;
                if (!existingList.isEmpty()) {
                    jadwal = existingList.get(0);
                } else {
                    jadwal = new PenjadwalanKonfirmasi();
                    jadwal.setRequestDelivery(rd);
                    jadwal.setTipeTugas(TipeTugas.PENGAMBILAN);
                    jadwal.setAlamatLengkap(rd.getAlamatLengkap());
                    jadwal.setAlamatMaps(rd.getAlamatMaps());
                    jadwal.setCatatan(rd.getKeterangan());
                    jadwal.setIsUrgen(rd.getIsUrgen() != null && rd.getIsUrgen());
                    jadwal.setMarketingName(rd.getCreator() != null ? rd.getCreator().getNama() : null);
                    jadwal.setStatusJadwal(StatusJadwal.MENUNGGU_KONFIRMASI);
                }

                jadwal.setPersonelId(request.getPersonelId());
                jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);

                if (request.getTanggalRencana() != null && !request.getTanggalRencana().isBlank()) {
                    try {
                        if (request.getTanggalRencana().contains("T")) {
                            jadwal.setTanggalJadwal(LocalDateTime.parse(request.getTanggalRencana(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate());
                        } else {
                            try {
                                jadwal.setTanggalJadwal(LocalDate.parse(request.getTanggalRencana(), DATE_FORMATTER));
                            } catch (Exception px) {
                                jadwal.setTanggalJadwal(LocalDate.parse(request.getTanggalRencana()));
                            }
                        }
                    } catch (Exception ignored) { }
                }

                penjadwalanRepo.save(jadwal);
                rd.setStatus(com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.MENUNGGU_PENGIRIMAN);
                requestDeliveryRepository.save(rd);
                count++;
            }
        }

        if (request.getMemoIds() != null) {
            for (String memoIdStr : request.getMemoIds()) {
                UUID memoId = UUID.fromString(memoIdStr);
                Memo memo = memoRepository.findById(memoId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

                List<PenjadwalanKonfirmasi> existingList = penjadwalanRepo.findAllByOrderByCreatedAtDesc().stream()
                        .filter(p -> p.getMemo() != null && p.getMemo().getId().equals(memoId))
                        .collect(Collectors.toList());

                PenjadwalanKonfirmasi jadwal;
                if (!existingList.isEmpty()) {
                    jadwal = existingList.get(0);
                } else {
                    jadwal = new PenjadwalanKonfirmasi();
                    jadwal.setMemo(memo);
                    jadwal.setTipeTugas(TipeTugas.PENGIRIMAN);
                    jadwal.setAlamatLengkap(memo.getCustomer() != null ? memo.getCustomer().getAlamatDefault() : null);
                    jadwal.setCatatan(memo.getDeskripsi());
                    jadwal.setStatusJadwal(StatusJadwal.MENUNGGU_KONFIRMASI);
                }

                jadwal.setPersonelId(request.getPersonelId());
                jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);

                if (request.getTanggalRencana() != null && !request.getTanggalRencana().isBlank()) {
                    try {
                        if (request.getTanggalRencana().contains("T")) {
                            jadwal.setTanggalJadwal(LocalDateTime.parse(request.getTanggalRencana(), DateTimeFormatter.ISO_DATE_TIME).toLocalDate());
                        } else {
                            try {
                                jadwal.setTanggalJadwal(LocalDate.parse(request.getTanggalRencana(), DATE_FORMATTER));
                            } catch (Exception px) {
                                jadwal.setTanggalJadwal(LocalDate.parse(request.getTanggalRencana()));
                            }
                        }
                    } catch (Exception ignored) { }
                }

                penjadwalanRepo.save(jadwal);
                memo.setStatusAkhir(MemoStatus.DIJADWALKAN);
                memoRepository.save(memo);
                
                memoLogRepository.save(new MemoLog(
                        memo.getId(),
                        MemoStatus.DIJADWALKAN.name(),
                        aktor.getId(),
                        "Memo dijadwalkan secara bulk oleh User ID: " + aktor.getId()
                ));
                count++;
            }
        }

        return WebResponse.<String>builder()
                .status(200)
                .message("Berhasil menjadwalkan " + count + " tugas")
                .data("OK")
                .build();
    }

    @Transactional
    public WebResponse<String> createBatchDropOff(BatchDropOffRequest request, String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        List<Memo> memos = new ArrayList<>();
        if (request.getMemoIds() != null && !request.getMemoIds().isEmpty()) {
            memos = memoRepository.findAllById(request.getMemoIds());
        }

        List<RequestDelivery> requests = new ArrayList<>();
        if (request.getRequestDeliveryIds() != null && !request.getRequestDeliveryIds().isEmpty()) {
            requests = requestDeliveryRepository.findAllById(request.getRequestDeliveryIds());
        }

        if (memos.isEmpty() && requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch tidak boleh kosong");
        }

        PenjadwalanKonfirmasi batch = PenjadwalanKonfirmasi.builder()
                .tipeTugas(TipeTugas.DROP_OFF_EKSPEDISI)
                .personelId(request.getPersonelId())
                .tanggalJadwal(LocalDate.parse(request.getTanggalJadwal(), DATE_FORMATTER))
                .estimasiWaktu(request.getEstimasiWaktu())
                .catatan(request.getCatatan())
                .alamatLengkap(request.getAlamatLengkap())
                .alamatMaps(request.getAlamatMaps())
                .idKodepos(request.getIdKodepos())
                .manualCustomerName(request.getManualCustomerName())
                .isExpeditionOutlet(true)
                .statusJadwal(StatusJadwal.DIJADWALKAN)
                .manifestMemos(memos)
                .manifestRequests(requests)
                .build();

        penjadwalanRepo.save(batch);

        // Update status all items
        for (Memo memo : memos) {
            memo.setStatusAkhir(MemoStatus.MENUNGGU_EXPEDISI);
            memoRepository.save(memo);
            memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.MENUNGGU_EXPEDISI.name(), aktor.getId(), 
                "Masuk Batch Drop-off Jadwal ID: " + batch.getId()));
        }

        for (RequestDelivery rd : requests) {
            rd.setStatus(com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.MENUNGGU_PENGIRIMAN);
            requestDeliveryRepository.save(rd);
        }

        return WebResponse.<String>builder()
                .status(201)
                .message("Batch Drop-off berhasil dibuat")
                .data(batch.getId().toString())
                .build();
    }

    @Transactional
    public WebResponse<String> bulkMulaiTugas(List<Long> ids, String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        int count = 0;
        int skipped = 0;
        for (Long id : ids) {
            PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(id).orElse(null);
            if (jadwal == null || (jadwal.getStatusJadwal() != StatusJadwal.DIJADWALKAN && 
                                   jadwal.getStatusJadwal() != StatusJadwal.MENUNGGU_KONFIRMASI)) {
                skipped++;
                continue;
            }

            jadwal.setStatusJadwal(StatusJadwal.DALAM_PENGIRIMAN);
            penjadwalanRepo.save(jadwal);

            if (jadwal.getMemo() != null) {
                Memo memo = jadwal.getMemo();
                memo.setStatusAkhir(MemoStatus.DALAM_PENGIRIMAN);
                memoRepository.save(memo);
                memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DALAM_PENGIRIMAN.name(), aktor.getId(), 
                    "Tugas " + jadwal.getTipeTugas() + " dimulai secara bulk oleh User: " + aktor.getNama()));
            } else if (jadwal.getRequestDelivery() != null) {
                RequestDelivery rd = jadwal.getRequestDelivery();
                rd.setStatus(com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.DALAM_PENGIRIMAN);
                requestDeliveryRepository.save(rd);
            }
            count++;
        }

        String finalMessage = "Berhasil memulai " + count + " tugas pengiriman.";
        if (skipped > 0) {
            finalMessage += " (" + skipped + " dilewati karena status tidak sesuai)";
        }

        return WebResponse.<String>builder()
                .status(200)
                .message(finalMessage)
                .data("OK")
                .build();
    }

    @Transactional
    public WebResponse<String> bulkSelesaikanTugas(List<Long> ids, MultipartFile photo, String namaPenerima, String catatanOperasional, String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto bukti wajib diunggah");
        }
        if (namaPenerima == null || namaPenerima.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama penerima wajib diisi");
        }

        String fileName;
        try {
            fileName = fileService.saveMemoPhoto(photo);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menyimpan foto bukti");
        }

        int count = 0;
        int skipped = 0;
        for (Long id : ids) {
            PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findById(id).orElse(null);
            if (jadwal == null) {
                skipped++;
                continue;
            }
            if (jadwal.getStatusJadwal() != StatusJadwal.DALAM_PENGIRIMAN && jadwal.getStatusJadwal() != StatusJadwal.DIJADWALKAN) {
                skipped++;
                continue;
            }

            jadwal.setStatusJadwal(StatusJadwal.SELESAI);
            jadwal.setFotoBukti(fileName);
            jadwal.setNamaPenerima(namaPenerima);
            jadwal.setCatatanOperasional(catatanOperasional);
            penjadwalanRepo.save(jadwal);

            if (jadwal.getMemo() != null) {
                Memo memo = jadwal.getMemo();
                memo.setStatusAkhir(MemoStatus.DITERIMA_USER);
                memoRepository.save(memo);
                memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DITERIMA_USER.name(), aktor.getId(), 
                    "Selesai kirim bulk oleh User: " + aktor.getNama() + " (Penerima: " + namaPenerima + ")"));
            } else if (jadwal.getRequestDelivery() != null) {
                RequestDelivery rd = jadwal.getRequestDelivery();
                rd.setStatus(com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.SELESAI);
                requestDeliveryRepository.save(rd);
            }
            count++;
        }

        String finalMessage = "Berhasil menyelesaikan " + count + " tugas pengiriman.";
        if (skipped > 0) {
            finalMessage += " (" + skipped + " dilewati karena status tidak sesuai)";
        }

        return WebResponse.<String>builder()
                .status(200)
                .message(finalMessage)
                .data("OK")
                .build();
    }
}
