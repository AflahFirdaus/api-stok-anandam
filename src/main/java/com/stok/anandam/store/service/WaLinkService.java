package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import com.stok.anandam.store.core.postgres.repository.TransaksiServisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaLinkService {

    private final TransaksiServisRepository transaksiRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public String generateWaLink(UUID transaksiId, String tipePesan) {
        // 1. Cari data transaksi
        TransaksiServis transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan"));

        // 2. Ambil data yang dibutuhkan
        String nomorHp = formatNomorHp(transaksi.getPelanggan().getNoTelepon()); // Sesuaikan nama getter-nya
        String nama = transaksi.getPelanggan().getNamaPelanggan(); // Sesuaikan nama getter-nya
        String idNota = transaksi.getId().toString();
        String trackingToken = transaksi.getTrackingToken().toString();
        
        // Link untuk nota (PDF) dan link untuk tracking web
        String linkNota = baseUrl + "/api/v1/nota/download/" + idNota;
        String linkTracking = baseUrl + "/track/" + trackingToken;
        
        // 3. Susun template pesan berdasarkan tipe
        String pesan = "";
        
        if ("DITERIMA".equalsIgnoreCase(tipePesan)) {
            // Format tanggal: "21 Juni 2024 jam 11:12 WIB"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'jam' HH:mm 'WIB'", new Locale("id", "ID"));
            String tanggalMasuk = transaksi.getCreatedAt() != null ? transaksi.getCreatedAt().format(formatter) : "-";

            pesan = "*Notifikasi | ANANDAM ID (ANANDAM INDONESIA)*\n\n" +
                    "Barang Servis *" + transaksi.getJenisBarang() + " " + (transaksi.getMerek() != null ? transaksi.getMerek() : "") + "* telah diterima oleh ANANDAM ID (ANANDAM INDONESIA) " +
                    "dengan No. Servis *" + idNota + "* pada tanggal " + tanggalMasuk + ". " +
                    "Untuk Cek Status (Tracking) Servis barang Anda, silahkan buka Link dibawah ini. Terima Kasih.\n\n" +
                    linkTracking;
                    
        } else if ("SELESAI".equalsIgnoreCase(tipePesan)) {
            pesan = "Halo Kak *" + nama + "*,\n\n" +
                    "Servis barang Anda (*" + transaksi.getJenisBarang() + " " + (transaksi.getMerek() != null ? transaksi.getMerek() : "") + "*) di Anandam Computer sudah *SELESAI* dan siap diambil.\n\n" +
                    "Nota digital Anda dapat diunduh/dilihat di sini:\n" + linkNota + "\n\n" +
                    "Silakan tunjukkan nota tersebut saat pengambilan. Terima Kasih!";
                    
        } else if ("KENDALA".equalsIgnoreCase(tipePesan)) {
            pesan = "Halo Kak *" + nama + "*,\n\n" +
                    "Ada informasi terbaru mengenai servis barang Anda (*" + transaksi.getJenisBarang() + " " + (transaksi.getMerek() != null ? transaksi.getMerek() : "") + "*). " +
                    "Mohon cek link tracking berikut atau hubungi kami kembali untuk konfirmasi tindakan selanjutnya.\n\n" +
                    "Link Tracking: " + linkTracking + "\n\n" +
                    "Terima Kasih, Anandam Computer.";
        }

        // 4. Encode pesan agar aman dimasukkan ke dalam link URL
        String encodedPesan = URLEncoder.encode(pesan, StandardCharsets.UTF_8).replace("+", "%20");
        
        // 5. Kembalikan link WhatsApp yang utuh
        return "https://wa.me/" + nomorHp + "?text=" + encodedPesan;
    }

    private String formatNomorHp(String nomor) {
        if (nomor != null && nomor.startsWith("0")) {
            return "62" + nomor.substring(1);
        }
        return nomor;
    }
}