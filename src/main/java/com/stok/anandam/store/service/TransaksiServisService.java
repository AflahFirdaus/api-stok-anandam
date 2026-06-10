package com.stok.anandam.store.service;

import java.math.BigDecimal;
import com.stok.anandam.store.core.postgres.model.*;
import com.stok.anandam.store.core.postgres.repository.KlaimDistributorRepository;
import com.stok.anandam.store.core.postgres.repository.PelangganServisRepository;
import com.stok.anandam.store.core.postgres.repository.RiwayatTrackingRepository;
import com.stok.anandam.store.core.postgres.repository.TransaksiServisRepository;
import com.stok.anandam.store.core.postgres.repository.TransaksiServisSpecification;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.CreateTransaksiRequest;
import com.stok.anandam.store.dto.UpdateStatusServisRequest;
import com.stok.anandam.store.dto.TransaksiServisResponse;
import com.stok.anandam.store.dto.RiwayatServisUserResponse;
import com.stok.anandam.store.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransaksiServisService {
    private final KlaimDistributorRepository klaimDistributorRepository;
    private final TransaksiServisRepository transaksiRepository;
    private final PelangganServisRepository pelangganRepository;
    private final RiwayatTrackingRepository riwayatRepository;
    private final UserRepository userRepository; // Tambahan untuk memvalidasi user dari JWT
    private final AuditTrailService auditTrailService;

    @Transactional
    public TransaksiServisResponse buatTransaksiBaru(CreateTransaksiRequest request, String usernamePenerima) {
        // 1. Cari data Pelanggan
        PelangganServis pelanggan = pelangganRepository.findById(request.getPelangganId())
                .orElseThrow(() -> new ResourceNotFoundException("Pelanggan tidak ditemukan dengan ID: " + request.getPelangganId()));

        // 2. Cari data Admin/Teknisi yang login via JWT
        User penerima = userRepository.findByUsername(usernamePenerima)
                .orElseThrow(() -> new ResourceNotFoundException("Sesi tidak valid, User tidak ditemukan di database."));

        // 3. Tentukan status awal berdasarkan tipeNota
        StatusServis statusAwal;
        String catatanAwal;
        if ("KLAIM".equalsIgnoreCase(request.getTipeNota())) {
            statusAwal = StatusServis.KLAIM_MENUNGGU_PENGIRIMAN;
            catatanAwal = "Klaim distributor telah dicatat. Barang menunggu proses pengiriman ke distributor.";
        } else {
            statusAwal = StatusServis.BELUM_CEK;
            catatanAwal = "Barang telah diterima di toko dan sedang antre untuk pengecekan awal.";
        }

        // 4. Buat Transaksi
        TransaksiServis transaksi = TransaksiServis.builder()
                .noServis(generateNoServis())
                .pelanggan(pelanggan)
                .penerima(penerima)
                .jenisBarang(request.getJenisBarang())
                .merek(request.getMerek())
                .modelSeri(request.getModelSeri())
                .kelengkapan(request.getKelengkapan())
                .kerusakan(request.getKerusakan())
                .dp(request.getDp() != null ? request.getDp() : BigDecimal.ZERO)
                .estimasiBiaya(request.getEstimasiBiaya() != null ? request.getEstimasiBiaya() : BigDecimal.ZERO)
                .statusTerkini(statusAwal)
                .statusBayar(StatusPembayaran.BELUM_LUNAS)
                .build();
        
        transaksi = transaksiRepository.save(transaksi);

        // 5. Buat Log Riwayat Awal Otomatis
        RiwayatTracking riwayat = RiwayatTracking.builder()
                .transaksi(transaksi)
                .statusLog(statusAwal)
                .catatanPublik(catatanAwal)
                .diupdateOleh(penerima)
                .build();
        
        riwayatRepository.save(riwayat);

        return TransaksiServisResponse.fromEntity(transaksi);
    }

    @Transactional
    public TransaksiServisResponse updateStatusServis(UUID transaksiId, UpdateStatusServisRequest request, String usernamePengubah) {
        // 1. Cari Transaksi
        TransaksiServis transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan"));
        
        // 2. Cari data Admin/Teknisi
        User pengubah = userRepository.findByUsername(usernamePengubah)
                .orElseThrow(() -> new ResourceNotFoundException("Sesi tidak valid, User tidak ditemukan."));

        // 3. Validasi Transisi & Audit
        StatusServis statusLama = transaksi.getStatusTerkini();
        validateStatusTransition(statusLama, request.getStatusBaru());
        
        auditTrailService.logChange("transaksi_servis", transaksiId.toString(),
                                    "status_terkini", statusLama.getValue(),
                                    request.getStatusBaru().getValue(), usernamePengubah);

        // 4. Update Status (Hanya sekali update)
        transaksi.setStatusTerkini(request.getStatusBaru());
        
        // --- FIX BUG 2: Update field-field operasional lainnya ---
        if (request.getKondisiServis() != null) transaksi.setKondisiServis(request.getKondisiServis());
        if (request.getKetTindakan() != null) transaksi.setKetTindakan(request.getKetTindakan());
        
        if (request.getTeknisiId() != null) {
            User teknisi = userRepository.findById(request.getTeknisiId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teknisi dengan ID " + request.getTeknisiId() + " tidak ditemukan"));
            transaksi.setTeknisi(teknisi);
        }
        
        if (request.getPenyerahId() != null) {
            User penyerah = userRepository.findById(request.getPenyerahId())
                    .orElseThrow(() -> new ResourceNotFoundException("Admin Penyerah dengan ID " + request.getPenyerahId() + " tidak ditemukan"));
            transaksi.setPenyerah(penyerah);
        }
        
        if (request.getPengambilNama() != null) transaksi.setPengambilNama(request.getPengambilNama());
        if (request.getDurasiGaransi() != null) transaksi.setDurasiGaransi(request.getDurasiGaransi());
        if (request.getEstimasiBiaya() != null) transaksi.setEstimasiBiaya(request.getEstimasiBiaya());
        if (request.getBiayaFinal() != null) transaksi.setBiayaFinal(request.getBiayaFinal());
        if (request.getModalSparepart() != null) transaksi.setModalSparepart(request.getModalSparepart());
        if (request.getStatusBayar() != null) transaksi.setStatusBayar(request.getStatusBayar());
        if (request.getTglJatuhTempo() != null) transaksi.setTglJatuhTempo(request.getTglJatuhTempo());
        
        // --- FIX BUG 1: Sinkronisasi tglJatuhTempo -> tglBatasGaransi ketika SUDAH_DIAMBIL ---
        if (request.getStatusBaru() == StatusServis.SUDAH_DIAMBIL) {
            if (request.getTglJatuhTempo() != null) {
                // Mapping dari LocalDate tglJatuhTempo ke LocalDateTime tglBatasGaransi (pukul 23:59:59 di hari tersebut)
                LocalDate jatuhTempo = request.getTglJatuhTempo();
                LocalDateTime batasGaransi = jatuhTempo.atTime(23, 59, 59);
                transaksi.setTglBatasGaransi(batasGaransi);
                log.info("[UPDATE_STATUS][{}] Sinkronisasi tglBatasGaransi dari tglJatuhTempo: {} -> {}", 
                         transaksi.getNoServis(), jatuhTempo, batasGaransi);
            } else if (transaksi.getTglJatuhTempo() != null && transaksi.getTglBatasGaransi() == null) {
                // Fallback: mapping data lama jika request tidak membawa tglJatuhTempo tapi sudah ada di DB
                LocalDate existingJatuhTempo = transaksi.getTglJatuhTempo();
                LocalDateTime batasGaransi = existingJatuhTempo.atTime(23, 59, 59);
                transaksi.setTglBatasGaransi(batasGaransi);
                log.warn("[UPDATE_STATUS][{}] Sinkronisasi tglBatasGaransi dari existing tglJatuhTempo: {} -> {}",
                         transaksi.getNoServis(), existingJatuhTempo, batasGaransi);
            }
            
            // Validasi: pastikan tglBatasGaransi sudah terisi sebelum .save()
            if (transaksi.getTglBatasGaransi() == null) {
                log.error("[UPDATE_STATUS][{}] tglBatasGaransi masih NULL setelah update ke SUDAH_DIAMBIL! tglJatuhTempo juga NULL.", 
                         transaksi.getNoServis());
            } else {
                log.info("[UPDATE_STATUS][{}] Validasi: tglBatasGaransi terisi = {}", 
                         transaksi.getNoServis(), transaksi.getTglBatasGaransi());
            }
        }
        // ---------------------------------------------------------

        String finalCatatanPublik = request.getCatatanPublikLog();
        if (request.getModelSeriBaru() != null && !request.getModelSeriBaru().trim().isEmpty()) {
            String oldSn = transaksi.getModelSeri() != null ? transaksi.getModelSeri() : "-";
            // Simpan SN lama ke field modelSeriLama agar tetap teraudit di detail barang
            transaksi.setModelSeriLama(oldSn);
            transaksi.setModelSeri(request.getModelSeriBaru());
            String infoGantiUnit = "Unit diganti baru. SN Lama: " + oldSn + " -> SN Baru: " + request.getModelSeriBaru();
            if (finalCatatanPublik == null || finalCatatanPublik.isEmpty()) {
                finalCatatanPublik = infoGantiUnit;
            } else {
                finalCatatanPublik = finalCatatanPublik + "\n\n" + infoGantiUnit;
            }
            auditTrailService.logChange("transaksi_servis", transaksiId.toString(),
                                        "model_seri", oldSn,
                                        request.getModelSeriBaru(), usernamePengubah);
        }
        
        transaksi = transaksiRepository.save(transaksi);

        // 5. Catat Log Perubahan
        RiwayatTracking riwayat = RiwayatTracking.builder()
                .transaksi(transaksi)
                .statusLog(request.getStatusBaru())
                .catatanPublik(finalCatatanPublik)
                .diupdateOleh(pengubah)
                .build();
        
        riwayatRepository.save(riwayat);

        return TransaksiServisResponse.fromEntity(transaksi);
    }

    @Transactional(readOnly = true)
    public List<TransaksiServisResponse> getServisGaransiAktif() {
        LocalDateTime now = LocalDateTime.now();
        return transaksiRepository.findAktifGaransi(now)
                .stream()
                .map(TransaksiServisResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransaksiServisResponse> getServisGaransiExpired() {
        LocalDateTime now = LocalDateTime.now();
        return transaksiRepository.findExpiredGaransi(now)
                .stream()
                .map(TransaksiServisResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ===== NEW: Paginated + Search Methods =====

    /**
     * Mencari transaksi servis berdasarkan status dan keyword pencarian (noServis / namaPelanggan).
     * Mendukung pagination (page & size).
     */
    @Transactional(readOnly = true)
    public Page<TransaksiServisResponse> getServisByStatus(String search, StatusServis status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<TransaksiServis> spec = Specification
                .where(TransaksiServisSpecification.hasStatus(status))
                .and(TransaksiServisSpecification.searchByNoServisOrNamaPelanggan(search));

        return transaksiRepository.findAll(spec, pageable)
                .map(TransaksiServisResponse::fromEntity);
    }

    /**
     * Mencari transaksi garansi aktif dengan keyword pencarian (noServis / namaPelanggan).
     * Mendukung pagination (page & size).
     */
    @Transactional(readOnly = true)
    public Page<TransaksiServisResponse> getServisGaransiAktif(String search, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "tglBatasGaransi"));

        Specification<TransaksiServis> spec = Specification
                .where(TransaksiServisSpecification.isGaransiAktif(now))
                .and(TransaksiServisSpecification.searchByNoServisOrNamaPelanggan(search));

        return transaksiRepository.findAll(spec, pageable)
                .map(TransaksiServisResponse::fromEntity);
    }

    /**
     * Mencari transaksi garansi expired dengan keyword pencarian (noServis / namaPelanggan).
     * Mendukung pagination (page & size).
     */
    @Transactional(readOnly = true)
    public Page<TransaksiServisResponse> getServisGaransiExpired(String search, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tglBatasGaransi"));

        Specification<TransaksiServis> spec = Specification
                .where(TransaksiServisSpecification.isGaransiExpired(now))
                .and(TransaksiServisSpecification.searchByNoServisOrNamaPelanggan(search));

        return transaksiRepository.findAll(spec, pageable)
                .map(TransaksiServisResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public KlaimDistributor getKlaimByTransaksiId(UUID transaksiId) {
        return klaimDistributorRepository.findByTransaksiId(transaksiId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public TransaksiServisResponse getTransaksiById(UUID transaksiId) {
        TransaksiServis transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi servis tidak ditemukan dengan ID: " + transaksiId));
        return TransaksiServisResponse.fromEntity(transaksi);
    }

    /**
     * Mendapatkan riwayat servis lengkap untuk seorang pelanggan (user).
     * Menampilkan informasi pelanggan, total jumlah servis, total biaya, dan detail setiap transaksi.
     *
     * @param pelangganId UUID pelanggan yang ingin dicek riwayatnya
     * @return RiwayatServisUserResponse berisi data rekap dan detail transaksi
     */
    @Transactional(readOnly = true)
    public RiwayatServisUserResponse getRiwayatServisByPelangganId(UUID pelangganId) {
        PelangganServis pelanggan = pelangganRepository.findById(pelangganId)
                .orElseThrow(() -> new ResourceNotFoundException("Pelanggan tidak ditemukan dengan ID: " + pelangganId));

        List<TransaksiServis> transaksiList = transaksiRepository.findByPelangganIdOrderByCreatedAtDesc(pelangganId);

        return RiwayatServisUserResponse.fromEntity(pelanggan, transaksiList);
    }

    @Transactional
    public KlaimDistributor buatKlaimDistributor(UUID transaksiId, com.stok.anandam.store.dto.CreateKlaimRequest request, String usernameAdmin) {
        // 1. Cari Transaksi & Validasi Admin via JWT
        TransaksiServis transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi servis tidak ditemukan"));
        
        User admin = userRepository.findByUsername(usernameAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Sesi tidak valid"));

        // 2. Simpan Data Logistik Klaim
        KlaimDistributor klaim = KlaimDistributor.builder()
                .transaksi(transaksi)
                .namaDistributor(request.getNamaDistributor())
                .alamatDistributor(request.getAlamatDistributor())
                .resiPengiriman(request.getResiPengiriman())
                .biayaKlaim(request.getBiayaKlaim() != null ? request.getBiayaKlaim() : java.math.BigDecimal.ZERO)
                .createdBy(admin)
                .build();
        klaim = klaimDistributorRepository.save(klaim);

        // 3. Ubah Status Transaksi Menjadi Menunggu Pengiriman
        transaksi.setStatusTerkini(StatusServis.KLAIM_MENUNGGU_PENGIRIMAN);
        transaksiRepository.save(transaksi);

        // 4. Catat Riwayat Tracking (Ini yang akan ter-masking otomatis di publik)
        RiwayatTracking riwayat = RiwayatTracking.builder()
                .transaksi(transaksi)
                .statusLog(StatusServis.KLAIM_MENUNGGU_PENGIRIMAN)
                .catatanInternal("Proses Klaim dibuat untuk tujuan: " + request.getNamaDistributor())
                .catatanPublik("Barang sedang dipersiapkan untuk dikirim ke distributor pusat / service center resmi.")
                .diupdateOleh(admin) // <-- SAKRAL: Ubah dari .userUpdate ke .diupdateOleh agar tidak compile error
                .build();
        riwayatRepository.save(riwayat);

        return klaim;
    }

    @Transactional
    public KlaimDistributor updateStatusKlaimLogistik(UUID klaimId, StatusServis statusBaru, String catatanInternal, String catatanPublik, String nomorResiBaru, String usernameAdmin) {
        // 1. Cari data Klaim Logistik
        KlaimDistributor klaim = klaimDistributorRepository.findById(klaimId)
                .orElseThrow(() -> new ResourceNotFoundException("Data klaim distributor tidak ditemukan"));
        
        User admin = userRepository.findByUsername(usernameAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("Sesi tidak valid"));

        // 2. Ambil Transaksi Servis Terkait
        TransaksiServis transaksi = klaim.getTransaksi();

        // 3. Logika Otomatisasi Tanggal Berdasarkan State Status
        if (statusBaru == StatusServis.KLAIM_DIKIRIM) {
            klaim.setTanggalKirim(java.time.LocalDateTime.now());
            if (nomorResiBaru != null) {
                klaim.setResiPengiriman(nomorResiBaru);
            }
        } else if (statusBaru == StatusServis.KLAIM_SUDAH_DIAMBIL) {
            klaim.setTanggalKembali(java.time.LocalDateTime.now());
            // Setelah diambil dari distributor, barang otomatis bersiap untuk diserahkan ke user atau masuk pengetesan toko
        }

        // 4. Update Status Utama di Transaksi Servis
        transaksi.setStatusTerkini(statusBaru);
        transaksiRepository.save(transaksi);
        klaimDistributorRepository.save(klaim);

        // 5. Suntikkan ke History Audit Trail (Riwayat Tracking)
        RiwayatTracking riwayat = RiwayatTracking.builder()
                .transaksi(transaksi)
                .statusLog(statusBaru)
                .catatanInternal(catatanInternal)
                .catatanPublik(catatanPublik)
                .diupdateOleh(admin)
                .build();
        riwayatRepository.save(riwayat);

        return klaim;
    }

    private String generateNoServis() {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "SRV-" + datePart + "-" + randomPart;
    }

    private void validateStatusTransition(StatusServis statusLama, StatusServis statusBaru) {
    if (statusLama == StatusServis.SUDAH_DIAMBIL || statusLama == StatusServis.BATAL) {
        throw new IllegalStateException("Transaksi sudah selesai/batal, tidak dapat diubah kembali.");
    }

    // 2. Jika status sama, izinkan (no-op) — untuk menangani update field lain tanpa mengubah status
    if (statusLama == statusBaru) {
        return;
    }

    // 3. Definisi aturan transisi
    boolean isValid = false;

    switch (statusLama) {
        case BELUM_CEK:
            isValid = (statusBaru == StatusServis.SEDANG_CEK || statusBaru == StatusServis.BATAL);
            break;
        case SEDANG_CEK:
            isValid = (statusBaru == StatusServis.SEDANG_DIKERJAKAN || statusBaru == StatusServis.TUNGGU_KONFIRMASI || statusBaru == StatusServis.BATAL);
            break;
        case SEDANG_DIKERJAKAN:
            isValid = (statusBaru == StatusServis.SEDANG_TES || statusBaru == StatusServis.TUNGGU_SPAREPART || statusBaru == StatusServis.TUNGGU_KONFIRMASI || statusBaru.name().startsWith("KLAIM_"));
            break;
        case SEDANG_TES: // FIX BUG 3
            isValid = (statusBaru == StatusServis.BISA_DIAMBIL || statusBaru == StatusServis.SEDANG_DIKERJAKAN || statusBaru == StatusServis.BATAL);
            break;
        case TUNGGU_KONFIRMASI: // FIX BUG 3
            isValid = (statusBaru == StatusServis.SEDANG_DIKERJAKAN || statusBaru == StatusServis.BATAL || statusBaru == StatusServis.BISA_DIAMBIL);
            break;
        case TUNGGU_SPAREPART:
            isValid = (statusBaru == StatusServis.SEDANG_DIKERJAKAN || statusBaru == StatusServis.BATAL);
            break;
        case BISA_DIAMBIL:
            isValid = (statusBaru == StatusServis.SUDAH_DIAMBIL);
            break;
        case KLAIM_MENUNGGU_PENGIRIMAN: // FIX BUG 3
            isValid = (statusBaru == StatusServis.KLAIM_DIKIRIM || statusBaru == StatusServis.BATAL);
            break;
        case KLAIM_DIKIRIM: // FIX BUG 3
            isValid = (statusBaru == StatusServis.KLAIM_SUDAH_DIKIRIM);
            break;
        case KLAIM_SUDAH_DIKIRIM: // FIX BUG 3
            isValid = (statusBaru == StatusServis.KLAIM_SUDAH_DIAMBIL);
            break;
        case KLAIM_SUDAH_DIAMBIL:
            isValid = (statusBaru == StatusServis.SEDANG_TES || statusBaru == StatusServis.SEDANG_DIKERJAKAN || statusBaru == StatusServis.BISA_DIAMBIL);
            break;
        // Tambahkan case lain sesuai kebutuhan untuk melengkapi matrix di atas
        default:
            isValid = true; // Atau false jika ingin lockdown total
    }

    if (!isValid) {
        throw new IllegalStateException("Perubahan status dari " + statusLama + " ke " + statusBaru + " tidak diizinkan.");
    }
    }
}