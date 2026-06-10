package com.stok.anandam.store.service;

import com.stok.anandam.store.dto.LaporanServisKeuanganResponse;
import org.springframework.stereotype.Service;

@Service
public class LaporanServisExportService {

    public String generateCsv(LaporanServisKeuanganResponse data) {
        StringBuilder csv = new StringBuilder();
        csv.append("Periode,Total Transaksi,Total Omzet,Total Modal,Laba Bersih\n");
        csv.append(data.getPeriode()).append(",")
           .append(data.getTotalTransaksiServis()).append(",")
           .append(data.getTotalOmzetServis()).append(",")
           .append(data.getTotalModalSparepart()).append(",")
           .append(data.getLabaBersihServis()).append("\n");
        return csv.toString();
    }
}