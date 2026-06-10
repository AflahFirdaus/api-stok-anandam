package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.TransaksiServis;
import com.stok.anandam.store.util.QrCodeGenerator;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class NotaPdfService {

    private final TemplateEngine templateEngine;

    public byte[] generatePdf(TransaksiServis transaksi) {
        Context context = new Context();
        context.setVariable("transaksi", transaksi);
        
        // --- TAMBAHKAN INI ---
        String linkTracking = "https://anandamcomputer.com/track/" + transaksi.getTrackingToken();
        context.setVariable("qrCodeBase64", QrCodeGenerator.generateBase64(linkTracking));
        
        // Memproses template nota-template.html
        String html = templateEngine.process("nota-template", context);
        
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Gagal generate PDF", e);
        }
    }

}