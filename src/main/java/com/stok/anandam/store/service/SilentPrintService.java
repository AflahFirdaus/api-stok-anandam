package com.stok.anandam.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SilentPrintService {

    private static final String DEFAULT_PRINTER_NAME = "PrinterOnline";

    /**
     * Menerima PDF dari Flutter, simpan ke file temp, lalu panggil perintah lp untuk mencetak.
     */
    public void printPdfSilent(MultipartFile pdfFile, String targetPrinter) {
        if (pdfFile.isEmpty()) {
            throw new IllegalArgumentException("File PDF dari aplikasi kosong atau rusak.");
        }

        String printerName = (targetPrinter != null && !targetPrinter.trim().isEmpty())
                ? targetPrinter.trim()
                : DEFAULT_PRINTER_NAME;

        Path tempFile = null;
        try (InputStream inputStream = pdfFile.getInputStream()) {
            // 1. Simpan PDF ke file temp
            tempFile = Files.createTempFile("print_", ".pdf");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF sementara disimpan di: {} ({} bytes)", tempFile, Files.size(tempFile));

            // 2. Bangun perintah lp
            // lp -d PRINTER_NAME -o fit-to-page file.pdf
            ProcessBuilder pb = new ProcessBuilder(
                    "lp",
                    "-d", printerName,
                    "-o", "fit-to-page",
                    tempFile.toAbsolutePath().toString()
            );

            // Gabung stderr ke stdout
            pb.redirectErrorStream(true);
            log.info("Menjalankan perintah: lp -d {} {}", printerName, tempFile.toAbsolutePath());

            // 3. Eksekusi
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            // 4. Baca output
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.exitValue();

            if (finished && exitCode == 0) {
                log.info("Print sukses dikirim ke printer '{}'. Output: {}", printerName, output);
            } else {
                log.error("Print gagal. Exit code: {}. Output: {}", exitCode, output);
                throw new RuntimeException(
                        "Perintah lp gagal (exit " + exitCode + "): " + output
                );
            }

        } catch (Exception e) {
            log.error("Gagal melakukan proses print: {}", e.getMessage(), e);
            throw new RuntimeException("Terjadi kesalahan saat memproses printer: " + e.getMessage());
        } finally {
            // Hapus file temp
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("File temp dihapus: {}", tempFile);
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    /**
     * Mengembalikan daftar printer yang terdaftar di CUPS via perintah lpstat.
     */
    public String getAvailablePrinters() {
        try {
            // Coba lpstat -e (printer yang terdeteksi)
            ProcessBuilder pb = new ProcessBuilder("lpstat", "-e");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            String output = new String(process.getInputStream().readAllBytes()).trim();

            if (finished && process.exitValue() == 0 && !output.isEmpty()) {
                // Format: setiap baris adalah nama printer
                StringBuilder list = new StringBuilder("Daftar Printer Terdeteksi:\n");
                for (String line : output.split("\n")) {
                    String name = line.trim();
                    if (!name.isEmpty()) {
                        list.append("- ").append(name).append("\n");
                    }
                }
                return list.toString();
            }

            // Fallback: coba lpstat -p (printer status)
            pb = new ProcessBuilder("lpstat", "-p");
            pb.redirectErrorStream(true);
            process = pb.start();
            finished = process.waitFor(10, TimeUnit.SECONDS);
            output = new String(process.getInputStream().readAllBytes()).trim();

            if (finished && process.exitValue() == 0 && !output.isEmpty()) {
                StringBuilder list = new StringBuilder("Daftar Printer Terdeteksi:\n");
                for (String line : output.split("\n")) {
                    // Format: "printer PRINTER_NAME is idle..."
                    if (line.startsWith("printer ")) {
                        String name = line.substring(8, line.indexOf(" ", 8)).trim();
                        if (!name.isEmpty()) {
                            list.append("- ").append(name).append("\n");
                        }
                    }
                }
                return list.toString();
            }

            return "Tidak ada printer terdeteksi di server ini.";

        } catch (Exception e) {
            log.error("Gagal mengambil daftar printer: {}", e.getMessage());
            return "Gagal memuat daftar printer: " + e.getMessage();
        }
    }
}