package com.stok.anandam.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.print.PrinterJob;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
@Slf4j
public class SilentPrintService {

    // Default kata kunci nama printer jika dari Flutter tidak mengirim nama spesifik
    private static final String DEFAULT_PRINTER_NAME = "L310";

    // --- 1. Fungsi Utama: Menerima PDF dari Controller dan Mencetak ---
    public void printPdfSilent(MultipartFile pdfFile, String targetPrinter) {
        if (pdfFile.isEmpty()) {
            throw new IllegalArgumentException("File PDF dari aplikasi kosong atau rusak.");
        }

        String printerKeyword = (targetPrinter != null && !targetPrinter.trim().isEmpty()) 
                ? targetPrinter 
                : DEFAULT_PRINTER_NAME;

        try (InputStream inputStream = pdfFile.getInputStream();
            PDDocument document = PDDocument.load(inputStream)) {

            log.info("PDF berhasil dimuat. Jumlah halaman: {}", document.getNumberOfPages());

            // Cek apakah ada printer yang terinstall di OS
            PrintService[] allPrinters = PrintServiceLookup.lookupPrintServices(null, null);
            
            if (allPrinters == null || allPrinters.length == 0) {
                log.warn("Tidak ada printer terdeteksi di server ini (mungkin headless/cloud environment).");
                log.warn("Menyimpan PDF ke folder 'printed_documents' sebagai fallback.");
                savePdfFallback(pdfFile);
                return;
            }

            log.info("Ditemukan {} printer di server.", allPrinters.length);
            for (PrintService p : allPrinters) {
                log.info("  - Printer tersedia: {}", p.getName());
            }

            // Cari printer sesuai keyword
            PrintService myPrinter = findPrinterByName(allPrinters, printerKeyword);
            
            if (myPrinter == null) {
                log.warn("Printer dengan nama mengandung '{}' tidak ditemukan. Gunakan printer pertama yang tersedia.", printerKeyword);
                // Fallback: gunakan printer default (printer pertama)
                if (allPrinters.length > 0) {
                    myPrinter = allPrinters[0];
                    log.info("Menggunakan printer default: {}", myPrinter.getName());
                } else {
                    throw new RuntimeException("Tidak ada printer sama sekali yang terdeteksi di server.");
                }
            }

            log.info("Mempersiapkan print ke: {}", myPrinter.getName());

            // Setup pekerjaan cetak ke OS
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPageable(new PDFPageable(document));
                job.setPrintService(myPrinter);
                // EKSEKUSI: Lempar ke Print Spooler OS (Langsung nge-print tanpa popup)
                job.print();
                log.info("Dokumen berhasil masuk ke antrean cetak printer.");
            } catch (Exception printEx) {
                log.error("Gagal mencetak via PrinterJob (kemungkinan headless environment): {}", printEx.getMessage());
                log.warn("Menyimpan PDF ke folder 'printed_documents' sebagai fallback karena printer tidak bisa diakses.");
                savePdfFallback(pdfFile);
            }

        } catch (Exception e) {
            log.error("Gagal melakukan proses print: {}", e.getMessage(), e);
            throw new RuntimeException("Terjadi kesalahan saat memproses printer: " + e.getMessage());
        }
    }

    /**
     * Fallback: simpan PDF ke folder printed_documents/ ketika printer tidak tersedia.
     */
    private void savePdfFallback(MultipartFile pdfFile) {
        try {
            Path outputDir = Paths.get("printed_documents");
            Files.createDirectories(outputDir);
            String fileName = "print_" + System.currentTimeMillis() + ".pdf";
            Path outputPath = outputDir.resolve(fileName);
            Files.copy(pdfFile.getInputStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF berhasil disimpan ke: {}", outputPath.toAbsolutePath());
        } catch (Exception saveEx) {
            log.error("Gagal menyimpan PDF fallback: {}", saveEx.getMessage());
            throw new RuntimeException("Gagal menyimpan PDF. Detail: " + saveEx.getMessage());
        }
    }

    // --- HELPER: Mencari Printer di OS berdasarkan nama ---
    private PrintService findPrinterByName(PrintService[] printServices, String keyword) {
        for (PrintService printer : printServices) {
            if (printer.getName().toLowerCase().contains(keyword.toLowerCase())) {
                return printer;
            }
        }
        return null;
    }
    
    // --- HELPER OPTIONAL: Untuk cek daftar printer yang tersedia (Berguna untuk debug) ---
    public String getAvailablePrinters() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        if (printServices == null || printServices.length == 0) {
            return "Tidak ada printer terdeteksi di server ini.";
        }
        StringBuilder list = new StringBuilder("Daftar Printer Terdeteksi:\n");
        for (PrintService printer : printServices) {
            list.append("- ").append(printer.getName()).append("\n");
        }
        return list.toString();
    }
}