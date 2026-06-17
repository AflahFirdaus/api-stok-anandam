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

            // Cari printer yang terhubung ke OS (Windows/Linux) tempat Spring Boot berjalan
            PrintService myPrinter = findPrinterByName(printerKeyword);
            
            if (myPrinter == null) {
                throw new RuntimeException("Printer dengan nama mengandung '" + printerKeyword + "' tidak terdeteksi di Server.");
            }

            log.info("Mempersiapkan print ke: {}", myPrinter.getName());

            // Setup pekerjaan cetak ke OS
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));
            job.setPrintService(myPrinter);

            // EKSEKUSI: Lempar ke Print Spooler OS (Langsung nge-print tanpa popup)
            job.print();
            
            log.info("Dokumen berhasil masuk ke antrean cetak printer.");

        } catch (Exception e) {
            log.error("Gagal melakukan proses print: {}", e.getMessage(), e);
            throw new RuntimeException("Terjadi kesalahan saat memproses printer: " + e.getMessage());
        }
    }

    // --- HELPER: Mencari Printer di OS berdasarkan nama ---
    private PrintService findPrinterByName(String keyword) {
        // Mengambil semua daftar printer yang terinstal di komputer/server ini
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        
        for (PrintService printer : printServices) {
            // Mengecek ignore-case (misal: "EPSON L310 Series" akan cocok dengan keyword "L310")
            if (printer.getName().toLowerCase().contains(keyword.toLowerCase())) {
                return printer;
            }
        }
        
        // Return null jika tidak ada yang cocok
        return null;
    }
    
    // --- HELPER OPTIONAL: Untuk cek daftar printer yang tersedia (Berguna untuk debug) ---
    public String getAvailablePrinters() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        StringBuilder list = new StringBuilder("Daftar Printer Terdeteksi:\n");
        for (PrintService printer : printServices) {
            list.append("- ").append(printer.getName()).append("\n");
        }
        return list.toString();
    }
}