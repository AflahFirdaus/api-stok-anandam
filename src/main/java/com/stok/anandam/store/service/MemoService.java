package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.*;
import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import com.stok.anandam.store.core.postgres.model.enums.StatusJadwal;
import com.stok.anandam.store.core.postgres.model.enums.TipeTugas;
import com.stok.anandam.store.core.postgres.repository.*;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;
    private final MemoLogRepository memoLogRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private void sendMemoRefreshSignal() {
        try {
            messagingTemplate.convertAndSend("/topic/memos", "REFRESH");
        } catch (Exception e) {
            // Jangan biarkan error websocket menggagalkan transaksi utama
            System.err.println("Gagal mengirim sinyal WebSocket: " + e.getMessage());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        
        // 1. Cek tipe file (MIME type)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File harus berupa gambar (JPEG/PNG)");
        }
        
        // 2. Cek ekstensi
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            int lastIndex = fileName.lastIndexOf(".");
            if (lastIndex == -1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tidak memiliki ekstensi");
            }
            String ext = fileName.substring(lastIndex + 1).toLowerCase();
            if (!java.util.List.of("jpg", "jpeg", "png", "webp").contains(ext)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ekstensi file tidak didukung. Gunakan JPG, PNG, atau WEBP.");
            }
        }
        
        // 3. Cek ukuran (maks 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukuran file maksimal adalah 5MB");
        }
    }
    private final PenjadwalanKonfirmasiRepository penjadwalanRepo;
    private final MemoItemRepository memoItemRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final KodeposRepository kodeposRepository;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Transactional(readOnly = true)
    public WebResponse<List<MemoDetailResponse>> getListMemoByStatus(MemoStatus status, String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        List<Memo> memos;
        if (status != null) {
            memos = memoRepository.findByStatusAkhirOrderByCreatedAtDesc(status);
        } else {
            memos = memoRepository.findAllByOrderByCreatedAtDesc();
        }

        // ROLE-BASED FILTERING
        String roleName = aktor.getRole() != null ? aktor.getRole().name() : "";
        String userEmpCode = aktor.getEmployeeCode(); 

        if (roleName.startsWith("MARKETING_") || "MARKETING".equals(roleName)) {
            List<UUID> assignedMemoIds = penjadwalanRepo.findByPersonelIdAndDeletedAtIsNull(aktor.getId())
                    .stream()
                    .filter(t -> t.getMemo() != null)
                    .map(t -> t.getMemo().getId())
                    .collect(Collectors.toList());

            memos = memos.stream()
                    .filter(m -> {
                        boolean isOwner = m.getMarketingEmpCode() != null && m.getMarketingEmpCode().equals(userEmpCode);
                        boolean isCreator = m.getCreator() != null && m.getCreator().getId().equals(aktor.getId());
                        boolean isSameRole = m.getCreator() != null && m.getCreator().getRole() == aktor.getRole();
                        boolean isAssigned = assignedMemoIds.contains(m.getId());
                        
                        if (m.getStatusAkhir() == MemoStatus.DRAFT) {
                            return isCreator || isSameRole;
                        }
                        return isOwner || isCreator || isSameRole || isAssigned;
                    })
                    .collect(Collectors.toList());
        } else if ("GUDANG".equals(roleName) || "SPV_GUDANG".equals(roleName)) {
            memos = memos.stream()
                    .filter(m -> 
                        m.getStatusAkhir() == MemoStatus.PENDING ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_PERSETUJUAN ||
                        m.getStatusAkhir() == MemoStatus.DISETUJUI ||
                        m.getStatusAkhir() == MemoStatus.DITOLAK ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_GUDANG ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_NOTA ||
                        m.getStatusAkhir() == MemoStatus.DIBUAT_NOTA ||
                        m.getStatusAkhir() == MemoStatus.KENDALA_BARANG ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_TEKNISI ||
                        m.getStatusAkhir() == MemoStatus.PROSES_TEKNISI ||
                        m.getStatusAkhir() == MemoStatus.BUFFER_ZONE ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_PENGIRIMAN ||
                        m.getStatusAkhir() == MemoStatus.DALAM_PENGIRIMAN ||
                        m.getStatusAkhir() == MemoStatus.TERKIRIM_SEBAGIAN ||
                        m.getStatusAkhir() == MemoStatus.SELESAI
                    )
                    .collect(Collectors.toList());
        } else if ("TEKNISI".equals(roleName) || "SPV_TEKNISI".equals(roleName)) {
            memos = memos.stream()
                    .filter(m -> 
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_TEKNISI ||
                        m.getStatusAkhir() == MemoStatus.PROSES_TEKNISI
                    )
                    .collect(Collectors.toList());
        } else if ("NOTA".equals(roleName)) {
            memos = memos.stream()
                    .filter(m -> m.getStatusAkhir() == MemoStatus.MENUNGGU_NOTA || m.getStatusAkhir() == MemoStatus.DIBUAT_NOTA)
                    .collect(Collectors.toList());
        } else if ("DELIVERY".equals(roleName)) {
            List<UUID> assignedMemoIds = penjadwalanRepo.findByPersonelIdAndDeletedAtIsNull(aktor.getId())
                    .stream()
                    .filter(t -> t.getMemo() != null)
                    .map(t -> t.getMemo().getId())
                    .collect(Collectors.toList());

            memos = memos.stream()
                    .filter(m -> assignedMemoIds.contains(m.getId()))
                    .collect(Collectors.toList());
        }

        List<MemoDetailResponse> responses = memos.stream().map(this::mapToDetailResponse).collect(Collectors.toList());

        return WebResponse.<List<MemoDetailResponse>>builder()
                .status(200)
                .message("Success")
                .data(responses)
                .build();
    }

    @Transactional(readOnly = true)
    public WebResponse<java.util.Map<String, Long>> getMemoCounts(String username) {
        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        
        // This is a naive implementation: fetching all and counting.
        // For production with many records, a custom GROUP BY query in repository is better.
        // But let's follow the existing pattern for now.
        List<Memo> allMemos = memoRepository.findAll();
        
        String roleName = aktor.getRole() != null ? aktor.getRole().name() : "";
        String userEmpCode = aktor.getEmployeeCode();
        
        // Filter by role like in getListMemoByStatus
        if (roleName.startsWith("MARKETING_") || "MARKETING".equals(roleName)) {
             allMemos = allMemos.stream()
                    .filter(m -> {
                        boolean isOwner = m.getMarketingEmpCode() != null && m.getMarketingEmpCode().equals(userEmpCode);
                        boolean isCreator = m.getCreator() != null && m.getCreator().getId().equals(aktor.getId());
                        boolean isSameRole = m.getCreator() != null && m.getCreator().getRole() == aktor.getRole();
                        if (m.getStatusAkhir() == MemoStatus.DRAFT) return isCreator || isSameRole;
                        return isOwner || isCreator || isSameRole;
                    })
                    .collect(Collectors.toList());
        } else if ("DELIVERY".equals(roleName)) {
            List<UUID> assignedMemoIds = penjadwalanRepo.findByPersonelIdAndDeletedAtIsNull(aktor.getId())
                    .stream()
                    .filter(t -> t.getMemo() != null)
                    .map(t -> t.getMemo().getId())
                    .collect(Collectors.toList());

            allMemos = allMemos.stream()
                    .filter(m -> assignedMemoIds.contains(m.getId()))
                    .collect(Collectors.toList());
        } else if ("TEKNISI".equals(roleName) || "SPV_TEKNISI".equals(roleName)) {
            // Technicians see all memos that need technician processing as a shared pool
            allMemos = allMemos.stream()
                    .filter(m -> m.getStatusAkhir() == MemoStatus.MENUNGGU_TEKNISI || 
                                 m.getStatusAkhir() == MemoStatus.PROSES_TEKNISI)
                    .collect(Collectors.toList());
        } else if ("GUDANG".equals(roleName) || "SPV_GUDANG".equals(roleName)) {
            allMemos = allMemos.stream()
                    .filter(m -> 
                        m.getStatusAkhir() == MemoStatus.PENDING ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_PERSETUJUAN ||
                        m.getStatusAkhir() == MemoStatus.DISETUJUI ||
                        m.getStatusAkhir() == MemoStatus.DITOLAK ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_GUDANG ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_NOTA ||
                        m.getStatusAkhir() == MemoStatus.DIBUAT_NOTA ||
                        m.getStatusAkhir() == MemoStatus.KENDALA_BARANG ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_TEKNISI ||
                        m.getStatusAkhir() == MemoStatus.PROSES_TEKNISI ||
                        m.getStatusAkhir() == MemoStatus.BUFFER_ZONE ||
                        m.getStatusAkhir() == MemoStatus.MENUNGGU_PENGIRIMAN ||
                        m.getStatusAkhir() == MemoStatus.DALAM_PENGIRIMAN ||
                        m.getStatusAkhir() == MemoStatus.TERKIRIM_SEBAGIAN ||
                        m.getStatusAkhir() == MemoStatus.SELESAI
                    )
                    .collect(Collectors.toList());
        }

        java.util.Map<String, Long> counts = allMemos.stream()
                .collect(Collectors.groupingBy(m -> m.getStatusAkhir().name(), Collectors.counting()));
        
        return WebResponse.<java.util.Map<String, Long>>builder()
                .status(200)
                .message("Success Fetch Status Counts")
                .data(counts)
                .build();
    }

    @Transactional(readOnly = true)
    public WebResponse<MemoDetailResponse> getMemoDetail(UUID memoId, String username) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        String roleName = aktor.getRole() != null ? aktor.getRole().name() : "";
        String userEmpCode = aktor.getEmployeeCode();
        
        if (roleName.startsWith("MARKETING_") || "MARKETING".equals(roleName) || "SPV_MARKETING".equals(roleName)) {
            boolean isOwner = memo.getMarketingEmpCode() != null && memo.getMarketingEmpCode().equals(userEmpCode);
            boolean isCreator = memo.getCreator() != null && memo.getCreator().getId().equals(aktor.getId());
            boolean isSameRole = memo.getCreator() != null && memo.getCreator().getRole() == aktor.getRole();
            
            boolean isAssigned = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memoId).stream()
                .anyMatch(t -> aktor.getId().equals(t.getPersonelId()));

            if (memo.getStatusAkhir() == MemoStatus.DRAFT) {
                if (!isCreator && !isSameRole) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke draft orang lain");
                }
            } else {
                if (!isOwner && !isCreator && !isSameRole && !isAssigned) {
                     throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke memo ini");
                }
            }
        } else if ("DELIVERY".equals(roleName)) {
            List<PenjadwalanKonfirmasi> assignedTasks = penjadwalanRepo.findByPersonelIdAndDeletedAtIsNullOrderByCreatedAtDesc(aktor.getId());
            boolean isAssigned = assignedTasks.stream()
                .anyMatch(t -> t.getMemo() != null && t.getMemo().getId().equals(memoId));
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke memo ini karena tidak ditugaskan kepada Anda");
            }
        } else if ("TEKNISI".equals(roleName) || "SPV_TEKNISI".equals(roleName)) {
            if (memo.getStatusAkhir() != MemoStatus.MENUNGGU_TEKNISI && memo.getStatusAkhir() != MemoStatus.PROSES_TEKNISI) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke memo dengan status ini");
            }
        }

        return WebResponse.<MemoDetailResponse>builder()
                .status(200)
                .message("Success")
                .data(mapToDetailResponse(memo))
                .build();
    }

    private MemoDetailResponse mapToDetailResponse(Memo memo) {
        List<MemoItemResponse> itemResponses = memoItemRepository.findByMemoId(memo.getId())
                .stream()
                .map(item -> MemoItemResponse.builder()
                        .id(item.getId())
                        .namaBarang(item.getNamaBarang())
                        .qty(item.getQty() != null ? item.getQty() : 0)
                        .qtyShipped(item.getQtyShipped() != null ? item.getQtyShipped() : 0)
                        .qtyRemaining((item.getQty() != null ? item.getQty() : 0) - (item.getQtyShipped() != null ? item.getQtyShipped() : 0))
                        .hargaSatuan(item.getHargaSatuan())
                        .subtotal(item.getSubtotal())
                        .catatanGudang(item.getCatatanGudang())
                        .status(item.getStatus())
                        .build()
                ).collect(Collectors.toList());

        List<MemoLogResponse> logResponses = memoLogRepository.findByMemoIdOrderByCreatedAtDesc(memo.getId())
                .stream()
                .map(log -> MemoLogResponse.builder()
                        .status(log.getStatus())
                        .aktorId(log.getAktorId())
                        .keterangan(log.getKeterangan())
                        .createdAt(log.getCreatedAt())
                        .build()
                ).collect(Collectors.toList());
        
        List<PenjadwalanResponse> penjadwalanResponses = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memo.getId())
                .stream()
                .map(this::mapToPenjadwalanResponse)
                .collect(Collectors.toList());

        MemoDetailResponse.MemoDetailResponseBuilder builder = MemoDetailResponse.builder()
                .id(memo.getId())
                .nomorMemo(memo.getNomorMemo())
                .tanggalMemo(memo.getTanggalMemo())
                .customerId(memo.getCustomer() != null ? memo.getCustomer().getId() : null)
                .customerName(memo.getCustomer() != null ? memo.getCustomer().getNamaPelanggan() : "Tanpa Nama")
                .customerPhone(memo.getCustomer() != null ? memo.getCustomer().getNoHp() : "-")
                .marketingId(memo.getMarketing() != null ? memo.getMarketing().getId() : null)
                .marketingName(memo.getMarketingName() != null ? memo.getMarketingName() : (memo.getMarketing() != null ? memo.getMarketing().getNama() : "System"))
                .marketingEmpCode(memo.getMarketingEmpCode())
                .marketingUsername(memo.getMarketing() != null ? memo.getMarketing().getUsername() : null)
                .creatorName(memo.getCreator() != null ? memo.getCreator().getNama() : "System")
                .statusAkhir(memo.getStatusAkhir())
                .totalHarga(memo.getTotalHarga() != null ? memo.getTotalHarga() : java.math.BigDecimal.ZERO)
                .deskripsi(memo.getDeskripsi())
                .nomorJl(memo.getNomorJl())
                .memoType(memo.getMemoType())
                .orderIdMarketplace(memo.getOrderIdMarketplace())
                .resi(memo.getResi())
                .ekspedisi(memo.getEkspedisi())
                .platform(memo.getPlatform())
                .tempo(memo.getTempo())
                .badanUsaha(memo.getBadanUsaha())
                .isTeknisRequired(java.lang.Boolean.TRUE.equals(memo.getIsTeknisRequired()))
                .isDeliveryRequired(java.lang.Boolean.TRUE.equals(memo.getIsDeliveryRequired()))
                .opsiPengiriman(memo.getOpsiPengiriman())
                .metodePembayaran(memo.getMetodePembayaran())
                .buktiFoto(memo.getBuktiFoto())
                .buktiFotoUrl(memo.getBuktiFoto() != null ? "/uploads/memos/" + memo.getBuktiFoto() : null)
                .kodePos(memo.getKodePos() != null ? memo.getKodePos() : 
                    (penjadwalanResponses.stream()
                        .filter(p -> p.getKodePos() != null)
                        .map(PenjadwalanResponse::getKodePos)
                        .findFirst()
                        .orElse(null)))
                .items(itemResponses)
                .logs(logResponses)
                .penjadwalanHistory(penjadwalanResponses);

        if (!penjadwalanResponses.isEmpty()) {
            PenjadwalanResponse lastJadwal = penjadwalanResponses.get(penjadwalanResponses.size() - 1);
            builder.desaKelurahan(lastJadwal.getDesaKelurahan())
                   .kecamatan(lastJadwal.getKecamatan())
                   .kabupatenKota(lastJadwal.getKabupatenKota());
        } else if (memo.getKodePos() != null && !memo.getKodePos().isBlank()) {
            kodeposRepository.findFirstByKodePos(memo.getKodePos()).ifPresent(kp -> {
                builder.desaKelurahan(kp.getDesaKelurahan())
                       .kecamatan(kp.getKecamatan())
                       .kabupatenKota(kp.getKabupatenKota());
            });
        }

        return builder.build();
    }

    private PenjadwalanResponse mapToPenjadwalanResponse(com.stok.anandam.store.core.postgres.model.PenjadwalanKonfirmasi p) {
        PenjadwalanResponse.PenjadwalanResponseBuilder builder = PenjadwalanResponse.builder()
                .id(p.getId())
                .memoId(p.getMemo().getId())
                .nomorMemo(p.getMemo().getNomorMemo())
                .tipeTugas(p.getTipeTugas())
                .statusJadwal(p.getStatusJadwal())
                .alamatLengkap(p.getAlamatLengkap())
                .alamatMaps(p.getAlamatMaps())
                .idKodepos(p.getIdKodepos())
                .estimasiWaktu(p.getEstimasiWaktu())
                .catatan(p.getCatatan())
                .tanggalJadwal(p.getTanggalJadwal() != null ? p.getTanggalJadwal().format(DATE_FORMATTER) : null)
                .personelId(p.getPersonelId())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .kecamatan(p.getKecamatan())
                .desaKelurahan(p.getDesaKelurahan())
                .kabupatenKota(p.getKabupatenKota());
        
        if (p.getKodepos() != null) {
            builder.kecamatan(p.getKodepos().getKecamatan())
                   .desaKelurahan(p.getKodepos().getDesaKelurahan())
                   .kabupatenKota(p.getKodepos().getKabupatenKota())
                   .kodePos(p.getKodepos().getKodePos());
        }

        if (p.getPersonelId() != null) {
            userRepository.findById(p.getPersonelId()).ifPresent(u -> {
                builder.personelName(u.getNama())
                       .personelRole(u.getRole() != null ? u.getRole().name() : "N/A");
            });
        }

        return builder.build();
    }

    @Transactional
    public WebResponse<String> createMemo(CreateMemoRequest request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        Customer customer;
        if (request.getNoHpCustomer() != null && !request.getNoHpCustomer().isBlank()) {
            customer = customerRepository.findByNoHpAndDeletedAtIsNull(request.getNoHpCustomer())
                    .orElseGet(() -> {
                        Customer newCust = Customer.builder()
                                .namaPelanggan(request.getNamaCustomer() != null ? request.getNamaCustomer() : "Pelanggan Baru")
                                .noHp(request.getNoHpCustomer())
                                .build();
                        return customerRepository.save(newCust);
                    });
        } else {
            customer = Customer.builder()
                    .namaPelanggan(request.getNamaCustomer() != null ? request.getNamaCustomer() : "Pelanggan Baru")
                    .build();
            customer = customerRepository.save(customer);
        }

        String nomorMemo = "MEMO-" + request.getMemoType().toUpperCase() + "-" + System.currentTimeMillis();

        Memo memo = Memo.builder()
                .nomorMemo(nomorMemo)
                .customer(customer)
                .marketingName(request.getNamaMarketing())
                .marketingEmpCode(request.getMarketingEmpCode())
                .creator(creator)
                .tanggalMemo(request.getTanggal() != null ? LocalDate.parse(request.getTanggal(), DATE_FORMATTER).atStartOfDay() : LocalDateTime.now())
                .isTeknisRequired(request.getIsTeknisi())
                .isDeliveryRequired(request.getMemoType().equalsIgnoreCase("ONLINE") || request.getIsKirim())
                .opsiPengiriman(request.getOpsiPengiriman())
                .metodePembayaran(request.getMetodePembayaran())
                .statusAkhir(MemoStatus.DRAFT)
                .totalHarga(request.getTotalHarga())
                .deskripsi(request.getDeskripsi())
                .memoType(request.getMemoType())
                .orderIdMarketplace(request.getOrderIdMarketplace())
                .resi(request.getResi())
                .ekspedisi(request.getEkspedisi())
                .platform(request.getPlatform())
                .kodePos(request.getKodePos())
                .tempo(request.getTempo())
                .badanUsaha(request.getBadanUsaha())
                .build();

        memo = memoRepository.save(memo);

        if (request.getItems() != null) {
            for (MemoItemRequest itemReq : request.getItems()) {
                MemoItem item = MemoItem.builder()
                        .memo(memo)
                        .namaBarang(itemReq.getNamaBarang())
                        .qty(itemReq.getQty())
                        .hargaSatuan(itemReq.getHargaSatuan())
                        .subtotal(itemReq.getSubtotal())
                        .status("NORMAL")
                        .catatanGudang(itemReq.getCatatan())
                        .qtyShipped(0)
                        .build();
                memoItemRepository.save(item);
            }
        }

        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DRAFT.name(), creator.getId(), "Memo Draft baru dibuat oleh " + username));

        return WebResponse.<String>builder()
                .data(memo.getId().toString())
                .status(HttpStatus.CREATED.value())
                .message("Memo berhasil dibuat dengan nomor " + nomorMemo)
                .build();
    }

    @Transactional
    public WebResponse<String> updateMemo(UUID memoId, CreateMemoRequest request, String username) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        if (memo.getStatusAkhir() != MemoStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hanya memo status DRAFT yang dapat diubah");
        }

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        // Permission check: creator or same role
        boolean isCreator = memo.getCreator() != null && memo.getCreator().getId().equals(aktor.getId());
        boolean isSameRole = memo.getCreator() != null && memo.getCreator().getRole() == aktor.getRole();
        
        if (!isCreator && !isSameRole && !aktor.getRole().name().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki hak untuk mengubah memo ini");
        }

        Customer customer;
        if (request.getNoHpCustomer() != null && !request.getNoHpCustomer().isBlank()) {
            customer = customerRepository.findByNoHpAndDeletedAtIsNull(request.getNoHpCustomer())
                    .orElseGet(() -> {
                        Customer newCust = Customer.builder()
                                .namaPelanggan(request.getNamaCustomer() != null ? request.getNamaCustomer() : "Pelanggan Baru")
                                .noHp(request.getNoHpCustomer())
                                .build();
                        return customerRepository.save(newCust);
                    });
        } else {
            customer = Customer.builder()
                    .namaPelanggan(request.getNamaCustomer() != null ? request.getNamaCustomer() : "Pelanggan Baru")
                    .build();
            customer = customerRepository.save(customer);
        }

        memo.setCustomer(customer);
        memo.setMarketingName(request.getNamaMarketing());
        memo.setMarketingEmpCode(request.getMarketingEmpCode());
        memo.setTanggalMemo(request.getTanggal() != null ? LocalDate.parse(request.getTanggal(), DATE_FORMATTER).atStartOfDay() : LocalDateTime.now());
        memo.setIsTeknisRequired(request.getIsTeknisi());
        memo.setIsDeliveryRequired(request.getMemoType().equalsIgnoreCase("ONLINE") || request.getIsKirim());
        memo.setOpsiPengiriman(request.getOpsiPengiriman());
        memo.setMetodePembayaran(request.getMetodePembayaran());
        memo.setTotalHarga(request.getTotalHarga());
        memo.setDeskripsi(request.getDeskripsi());
        memo.setMemoType(request.getMemoType());
        memo.setOrderIdMarketplace(request.getOrderIdMarketplace());
        memo.setResi(request.getResi());
        memo.setEkspedisi(request.getEkspedisi());
        memo.setPlatform(request.getPlatform());
        memo.setKodePos(request.getKodePos());
        memo.setTempo(request.getTempo());
        memo.setBadanUsaha(request.getBadanUsaha());

        memoRepository.save(memo);

        memoItemRepository.deleteByMemoId(memoId);

        if (request.getItems() != null) {
            for (MemoItemRequest itemReq : request.getItems()) {
                MemoItem item = MemoItem.builder()
                        .memo(memo)
                        .namaBarang(itemReq.getNamaBarang())
                        .qty(itemReq.getQty())
                        .hargaSatuan(itemReq.getHargaSatuan())
                        .subtotal(itemReq.getSubtotal())
                        .status("NORMAL")
                        .catatanGudang(itemReq.getCatatan())
                        .qtyShipped(0)
                        .build();
                memoItemRepository.save(item);
            }
        }

        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DRAFT.name(), aktor.getId(), "Memo Draft diperbarui oleh " + username));

        sendMemoRefreshSignal();

        return WebResponse.<String>builder()
                .data(memo.getId().toString())
                .status(HttpStatus.OK.value())
                .message("Memo berhasil diperbarui")
                .build();
    }

    @Transactional
    public WebResponse<String> updateResi(UUID memoId, UpdateResiRequest request, String username) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        String oldResi = memo.getResi() != null ? memo.getResi() : "-";
        memo.setResi(request.getResi());
        memoRepository.save(memo);

        memoLogRepository.save(new MemoLog(memo.getId(), memo.getStatusAkhir().name(), aktor.getId(), 
                "Nomor Resi diperbarui dari " + oldResi + " menjadi " + request.getResi() + " oleh " + username));

        sendMemoRefreshSignal();

        return WebResponse.<String>builder()
                .data("OK")
                .status(200)
                .message("Nomor Resi berhasil diperbarui")
                .build();
    }

    @Transactional
    public WebResponse<String> updateMemoStatus(UUID memoId, UpdateMemoStatusRequest request, String username) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        MemoStatus targetStatus = request.getTargetStatus();
        memo.setStatusAkhir(targetStatus);


        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), targetStatus.name(), aktor.getId(), request.getKeteranganLog()));

        // Audit Log
        activityLogService.log(username, "MEMO_STATUS_UPDATE", 
            "Mengubah status Memo " + memo.getNomorMemo() + " menjadi " + targetStatus);

        sendMemoRefreshSignal();

        return WebResponse.<String>builder()
                .data("OK")
                .status(200)
                .message("Status memo berhasil diubah menjadi " + targetStatus)
                .build();
    }

    @Transactional
    public WebResponse<String> addPenjadwalan(UUID memoId, CreatePenjadwalanRequest request, String username) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        TipeTugas tipe = TipeTugas.valueOf(request.getTipeTugas().toUpperCase());

        PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memoId).stream()
                .filter(j -> j.getTipeTugas() == tipe)
                .findFirst()
                .orElse(new PenjadwalanKonfirmasi());

        jadwal.setMemo(memo);
        jadwal.setTipeTugas(tipe);
        jadwal.setTanggalJadwal(request.getTanggalJadwal() != null ? LocalDate.parse(request.getTanggalJadwal(), DATE_FORMATTER) : LocalDate.now());
        jadwal.setAlamatLengkap(request.getAlamatLengkap());
        jadwal.setAlamatMaps(request.getAlamatMaps());
        jadwal.setLatitude(request.getLatitude());
        jadwal.setLongitude(request.getLongitude());
        jadwal.setIdKodepos(request.getIdKodepos());
        jadwal.setEstimasiWaktu(request.getEstimasiWaktu());
        jadwal.setCatatan(request.getCatatan());
        
        // Save regional data components
        jadwal.setKabupatenKota(request.getKabupatenKota());
        jadwal.setKecamatan(request.getKecamatan());
        jadwal.setDesaKelurahan(request.getDesaKelurahan());
        
        if (jadwal.getId() == null) {
            jadwal.setStatusJadwal(StatusJadwal.MENUNGGU_KONFIRMASI);
        }

        if (aktor.getRole().name().equals("GUDANG") || aktor.getRole().name().equals("ADMIN")) {
            jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);
        }

        penjadwalanRepo.save(jadwal);

        boolean isChanged = false;
        if (tipe == TipeTugas.PENGIRIMAN && !Boolean.TRUE.equals(memo.getIsDeliveryRequired())) {
            memo.setIsDeliveryRequired(true);
            memo.setOpsiPengiriman("DELIVERY");
            isChanged = true;
        } else if (tipe == TipeTugas.TEKNISI && !Boolean.TRUE.equals(memo.getIsTeknisRequired())) {
            memo.setIsTeknisRequired(true);
            isChanged = true;
        }

        if (isChanged) {
            memoRepository.save(memo);
        }

        String logMessage = "Permintaan penjadwalan " + tipe + " ditambahkan";
        if (jadwal.getStatusJadwal() == StatusJadwal.DIJADWALKAN) {
            logMessage = "Jadwal " + tipe + " telah dikonfirmasi oleh " + username;
        }
        memoLogRepository.save(new MemoLog(memo.getId(), memo.getStatusAkhir().name(), aktor.getId(), logMessage));

        sendMemoRefreshSignal();

        return WebResponse.<String>builder()
                .data("OK")
                .status(HttpStatus.CREATED.value())
                .message("Penjadwalan " + tipe + " berhasil disimpan")
                .build();
    }

    @Transactional
    public WebResponse<String> konfirmasiKirim(UUID memoId, String itemsJson, MultipartFile photo, String username) {
        KonfirmasiKirimRequest request;
        try {
            request = objectMapper.readValue(itemsJson, KonfirmasiKirimRequest.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format data item tidak valid");
        }

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data item kirim tidak boleh kosong");
        }

        for (ItemKirim itemInput : request.getItems()) {
            MemoItem item = memoItemRepository.findById(itemInput.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item dengan ID " + itemInput.getItemId() + " tidak ditemukan"));

            int qtyBaru = item.getQtyShipped() + itemInput.getQtyDikirimSaatIni();
            if (qtyBaru > item.getQty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kuantitas kirim melebihi batas pesanan untuk item: " + item.getNamaBarang());
            }

            item.setQtyShipped(qtyBaru);
            memoItemRepository.save(item);
        }

        List<MemoItem> allItems = memoItemRepository.findByMemoId(memoId);
        boolean isSemuaTerkirim = allItems.stream().allMatch(i -> i.getQtyShipped() >= i.getQty());
        MemoStatus targetStatus = isSemuaTerkirim ? MemoStatus.SELESAI : MemoStatus.TERKIRIM_SEBAGIAN;   
        memo.setStatusAkhir(targetStatus);

        if (photo != null && !photo.isEmpty()) {
            validateImageFile(photo);
            try {
                String fileName = fileService.saveMemoPhoto(photo);
                memo.setBuktiFoto(fileName);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menyimpan foto bukti");
            }
        }

        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), targetStatus.name(), aktor.getId(), "Konfirmasi pengiriman oleh " + username));
        
        // Audit Log
        activityLogService.log(username, "MEMO_DELIVERY_CONFIRMED", 
            "Konfirmasi pengiriman Memo " + memo.getNomorMemo() + ". Status akhir: " + targetStatus);
        
        syncTaskStatus(memo.getId(), StatusJadwal.SELESAI, null);

        sendMemoRefreshSignal();

        return WebResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Konfirmasi pengiriman berhasil disimpan")
                .data(memo.getId().toString())
                .build();
    }

    @Transactional
    public WebResponse<String> updateItemCatatan(Long itemId, UpdateItemCatatanRequest request, String username) {
        MemoItem item = memoItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        if (!aktor.getRole().name().equals("GUDANG") && !aktor.getRole().name().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Hanya bagian gudang yang bisa mengisi catatan item");
        }

        item.setCatatanGudang(request.getCatatanGudang());
        memoItemRepository.save(item);

        return WebResponse.<String>builder()
                .data("OK")
                .status(200)
                .message("Catatan gudang untuk item berhasil diperbarui")
                .build();
    }

    @Transactional
    public WebResponse<String> createPendingMemo(CreateMemoRequest request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        Customer customer;
        if (request.getNoHpCustomer() != null && !request.getNoHpCustomer().isBlank()) {
            customer = customerRepository.findByNoHpAndDeletedAtIsNull(request.getNoHpCustomer())
                    .orElseGet(() -> {
                        Customer newCust = Customer.builder()
                                .namaPelanggan(request.getNamaCustomer() != null ? request.getNamaCustomer() : "Pelanggan Baru")
                                .noHp(request.getNoHpCustomer())
                                .build();
                        return customerRepository.save(newCust);
                    });
        } else {
            customer = Customer.builder()
                    .namaPelanggan(request.getNamaCustomer() != null ? request.getNamaCustomer() : "Pelanggan Baru")
                    .build();
            customer = customerRepository.save(customer);
        }

        String nomorMemo = "MEMO-PENDING-" + request.getMemoType().toUpperCase() + "-" + System.currentTimeMillis();

        Memo memo = Memo.builder()
                .nomorMemo(nomorMemo)
                .customer(customer)
                .marketingName(request.getNamaMarketing())
                .marketingEmpCode(request.getMarketingEmpCode())
                .creator(creator)
                .tanggalMemo(LocalDateTime.now())
                .isTeknisRequired(false)
                .isDeliveryRequired(false)
                .statusAkhir(MemoStatus.MENUNGGU_PERSETUJUAN)
                .totalHarga(request.getTotalHarga())
                .deskripsi(request.getDeskripsi())
                .memoType(request.getMemoType())
                .badanUsaha(request.getBadanUsaha())
                .build();

        memo = memoRepository.save(memo);

        if (request.getItems() != null) {
            for (MemoItemRequest itemReq : request.getItems()) {
                MemoItem item = MemoItem.builder()
                        .memo(memo)
                        .namaBarang(itemReq.getNamaBarang())
                        .qty(itemReq.getQty())
                        .hargaSatuan(itemReq.getHargaSatuan())
                        .subtotal(itemReq.getSubtotal())
                        .status("NORMAL")
                        .qtyShipped(0)
                        .build();
                memoItemRepository.save(item);
            }
        }

        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.MENUNGGU_PERSETUJUAN.name(), creator.getId(), "Memo Pending baru diajukan oleh " + username));

        sendMemoRefreshSignal();

        return WebResponse.<String>builder()
                .data(memo.getId().toString())
                .status(HttpStatus.CREATED.value())
                .message("Memo Pending berhasil diajukan")
                .build();
    }

    @Transactional
    public WebResponse<String> approvePendingMemo(UUID memoId, String username) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        if (!aktor.getRole().name().equals("ADMIN") && 
            !aktor.getRole().name().equals("SPV_MARKETING") && 
            !aktor.getRole().name().equals("SPV_GUDANG") && 
            !aktor.getRole().name().equals("GUDANG")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Admin, Supervisor, atau Bagian Gudang yang dapat menyetujui memo");
        }

        memo.setStatusAkhir(MemoStatus.DISETUJUI);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DISETUJUI.name(), aktor.getId(), "Memo Pending disetujui oleh " + username));

        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo berhasil disetujui").build();
    }

    @Transactional
    public WebResponse<String> rejectPendingMemo(UUID memoId, String username, String alasan) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));

        User aktor = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        if (!aktor.getRole().name().equals("ADMIN") && 
            !aktor.getRole().name().equals("SPV_MARKETING") && 
            !aktor.getRole().name().equals("SPV_GUDANG") && 
            !aktor.getRole().name().equals("GUDANG")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Admin, Supervisor, atau Bagian Gudang yang dapat menolak memo");
        }

        memo.setStatusAkhir(MemoStatus.DITOLAK);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DITOLAK.name(), aktor.getId(), "Memo Pending ditolak oleh " + username + ". Alasan: " + alasan));

        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo Pending berhasil ditolak").build();
    }

    @Transactional
    public WebResponse<String> releasePendingMemo(UUID id, String username) {
        Memo memo = memoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setStatusAkhir(MemoStatus.DRAFT);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DRAFT.name(), aktor.getId(), "Memo Pending dirilis kembali ke Draft oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo berhasil dirilis").build();
    }

    @Transactional
    public WebResponse<String> continueToMemo(UUID id, UpdateMemoTypeRequest request, String username) {
        Memo memo = memoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setMemoType(request.getDetails().getMemoType());
        memo.setStatusAkhir(MemoStatus.MENUNGGU_GUDANG);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.MENUNGGU_GUDANG.name(), aktor.getId(), "Memo Pending dilanjutkan menjadi Memo " + request.getDetails().getMemoType() + " oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo berhasil dilanjutkan").build();
    }

    @Transactional
    public WebResponse<String> finishPendingMemo(UUID id, String username) {
        Memo memo = memoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setStatusAkhir(MemoStatus.SELESAI);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.SELESAI.name(), aktor.getId(), "Memo Pending diselesaikan oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo berhasil diselesaikan").build();
    }

    @Transactional
    public WebResponse<String> finalizeMemo(UUID memoId, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setStatusAkhir(MemoStatus.MENUNGGU_GUDANG);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.MENUNGGU_GUDANG.name(), aktor.getId(), "Memo difinalisasi ke Gudang oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo berhasil difinalisasi").build();
    }

    @Transactional
    public WebResponse<String> finishWarehouseProcess(UUID memoId, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setStatusAkhir(MemoStatus.MENUNGGU_NOTA);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.MENUNGGU_NOTA.name(), aktor.getId(), "Gudang selesai menyiapkan barang. Menunggu Nota/Invoice."));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Proses gudang selesai").build();
    }

    @Transactional
    public WebResponse<String> finishInvoicingProcess(UUID memoId, FinishNotaRequest request, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setNomorJl(request.getNomorJl());
        memo.setStatusAkhir(MemoStatus.DIBUAT_NOTA);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DIBUAT_NOTA.name(), aktor.getId(), "Invoice/Nota berhasil dibuat (JL: " + request.getNomorJl() + ") oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Invoice berhasil dibuat").build();
    }

    @Transactional
    public WebResponse<String> confirmDeliveryRoute(UUID memoId, FinishGudangRequest request, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        LocalDate tglJadwal = null;
        if (request.getTanggalJadwal() != null && !request.getTanggalJadwal().isBlank()) {
            tglJadwal = LocalDate.parse(request.getTanggalJadwal(), DATE_FORMATTER);
        }

        if (request.getDriverId() != null) {
            PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memoId).stream()
                    .filter(j -> j.getTipeTugas() == TipeTugas.PENGIRIMAN)
                    .findFirst()
                    .orElseGet(() -> {
                        PenjadwalanKonfirmasi newJadwal = new PenjadwalanKonfirmasi();
                        newJadwal.setMemo(memo);
                        newJadwal.setTipeTugas(TipeTugas.PENGIRIMAN);
                        return newJadwal;
                    });
            jadwal.setPersonelId(request.getDriverId());
            jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);
            if (tglJadwal != null) jadwal.setTanggalJadwal(tglJadwal);
            penjadwalanRepo.save(jadwal);
        }

        if (request.getTeknisiId() != null) {
            PenjadwalanKonfirmasi jadwal = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memoId).stream()
                    .filter(j -> j.getTipeTugas() == TipeTugas.TEKNISI)
                    .findFirst()
                    .orElseGet(() -> {
                        PenjadwalanKonfirmasi newJadwal = new PenjadwalanKonfirmasi();
                        newJadwal.setMemo(memo);
                        newJadwal.setTipeTugas(TipeTugas.TEKNISI);
                        return newJadwal;
                    });
            jadwal.setPersonelId(request.getTeknisiId());
            jadwal.setStatusJadwal(StatusJadwal.DIJADWALKAN);
            if (tglJadwal != null) jadwal.setTanggalJadwal(tglJadwal);
            penjadwalanRepo.save(jadwal);
        }

        MemoStatus targetStatus = MemoStatus.MENUNGGU_PENGIRIMAN;
        String logMsg = "Jalur pengiriman telah dikonfirmasi oleh " + username;

        if (request.getTeknisiId() != null) {
            targetStatus = MemoStatus.MENUNGGU_TEKNISI;
            logMsg = "Penugasan teknisi telah dikonfirmasi oleh " + username;
        }

        memo.setStatusAkhir(targetStatus);
        memoRepository.save(memo);

        memoLogRepository.save(new MemoLog(memo.getId(), targetStatus.name(), aktor.getId(), logMsg));

        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Penugasan/Rute berhasil dikonfirmasi").build();
    }

    @Transactional
    public WebResponse<String> confirmPickupRoute(UUID memoId, MultipartFile photo, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        if (photo != null && !photo.isEmpty()) {
            validateImageFile(photo);
            String fileName = "memo_pickup_" + memoId + "_" + System.currentTimeMillis() + ".jpg";
            try {
                java.nio.file.Path path = java.nio.file.Paths.get("uploads/memos/" + fileName);
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.write(path, photo.getBytes());
                memo.setBuktiFoto(fileName);
            } catch (java.io.IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal mengunggah foto: " + e.getMessage());
            }
        }

        memo.setStatusAkhir(MemoStatus.DITERIMA_USER);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DITERIMA_USER.name(), aktor.getId(), "Serah terima barang (Gudang) telah difoto oleh " + username + ". Status berubah menjadi Diterima User."));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Pickup berhasil difoto, status berubah menjadi Diterima User").build();
    }

    @Transactional
    public WebResponse<String> confirmPickupFinal(UUID memoId, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        String role = aktor.getRole().name();
        boolean isOwner = memo.getMarketing() != null && memo.getMarketing().getUsername().equals(username);
        boolean isAdminOrSpv = "ADMIN".equals(role) || "SPV_MARKETING".equals(role);

        if (!isOwner && !isAdminOrSpv) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses untuk konfirmasi selesai memo ini.");
        }

        memo.setStatusAkhir(MemoStatus.SELESAI);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.SELESAI.name(), aktor.getId(), "Serah terima barang dikonfirmasi SELESAI oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo berhasil diselesaikan").build();
    }

    @Transactional
    public WebResponse<String> reportPhysicalIssue(UUID memoId, UpdateMemoStatusRequest request, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setStatusAkhir(MemoStatus.KENDALA_BARANG);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.KENDALA_BARANG.name(), aktor.getId(), "Kendala barang dilaporkan oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Kendala berhasil dilaporkan").build();
    }

    @Transactional
    public WebResponse<String> forceComplete(UUID memoId, UpdateMemoStatusRequest request, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setStatusAkhir(MemoStatus.SELESAI);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.SELESAI.name(), aktor.getId(), "Memo diselesaikan secara paksa oleh Admin: " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo berhasil diselesaikan secara paksa").build();
    }

    @Transactional
    public WebResponse<String> finishTechnicianProcess(UUID memoId, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        
        MemoStatus targetStatus = MemoStatus.SELESAI;
        String logKeterangan = "Proses teknisi selesai untuk memo ini";

        if (java.lang.Boolean.TRUE.equals(memo.getIsDeliveryRequired())) {
            targetStatus = MemoStatus.BUFFER_ZONE;
            logKeterangan = "Proses teknisi selesai. Memo masuk ke Buffer Zone untuk pengiriman.";
        }

        // Mark the technician task as SELESAI in penjadwalan
        List<PenjadwalanKonfirmasi> tasks = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memoId);
        for (PenjadwalanKonfirmasi task : tasks) {
            if (task.getTipeTugas() == TipeTugas.TEKNISI && task.getStatusJadwal() != StatusJadwal.SELESAI) {
                task.setStatusJadwal(StatusJadwal.SELESAI);
                penjadwalanRepo.save(task);
            }
        }

        memo.setStatusAkhir(targetStatus);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), targetStatus.name(), aktor.getId(), logKeterangan));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Proses teknisi selesai").build();
    }

    @Transactional
    public WebResponse<String> finishDeliveryProcess(UUID memoId, MultipartFile photo, String catatan, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        String fileName = null;
        if (photo != null && !photo.isEmpty()) {
            validateImageFile(photo);
            fileName = "memo_delivery_" + memoId + "_" + System.currentTimeMillis() + ".jpg";
            try {
                java.nio.file.Path path = java.nio.file.Paths.get("uploads/memos/" + fileName);
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.write(path, photo.getBytes());
            } catch (java.io.IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal mengunggah foto: " + e.getMessage());
            }
        }
        memo.setStatusAkhir(MemoStatus.DITERIMA_USER);
        memo.setBuktiFoto(fileName);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.DITERIMA_USER.name(), aktor.getId(), "Pengiriman diselesaikan oleh User: " + aktor.getNama() + " (Diterima User). Catatan: " + (catatan != null ? catatan : "-")));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Pengiriman selesai, status: Diterima User").build();
    }

    @Transactional
    public WebResponse<String> completeMemo(UUID memoId, String username) {
        Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo tidak ditemukan"));
        User aktor = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));
        memo.setStatusAkhir(MemoStatus.SELESAI);
        memoRepository.save(memo);
        memoLogRepository.save(new MemoLog(memo.getId(), MemoStatus.SELESAI.name(), aktor.getId(), "Memo dinyatakan Selesai oleh " + username));
        
        sendMemoRefreshSignal();

        return WebResponse.<String>builder().data("OK").status(200).message("Memo selesai").build();
    }

    @Transactional
    public WebResponse<String> syncTaskStatus(UUID memoId, StatusJadwal status, String notes) {
        List<PenjadwalanKonfirmasi> existingTasks = penjadwalanRepo.findByMemo_IdAndDeletedAtIsNull(memoId);
        for (PenjadwalanKonfirmasi task : existingTasks) {
            task.setStatusJadwal(status);
            if (notes != null) {
                task.setCatatan((task.getCatatan() != null ? task.getCatatan() : "") + " | Sync Note: " + notes);
            }
            penjadwalanRepo.save(task);
        }
        return WebResponse.<String>builder()
                .data("OK")
                .status(200)
                .message("Tasks sync successfully")
                .build();
    }
}
