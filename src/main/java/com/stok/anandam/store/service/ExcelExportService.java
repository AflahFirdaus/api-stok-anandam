package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.Purchase;
import com.stok.anandam.store.core.postgres.model.Sales;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportSalesToExcel(List<Sales> salesList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales Report");

            // Header Row - All columns matching the UI display
            Row headerRow = sheet.createRow(0);
            String[] columns = { 
                "ID", "Tanggal", "No Nota", "Kode", "Nama User", "Dept", 
                "Barang", "Qty", "Harga", "Total",
                "HPP Satuan", "Total HPP", "Laba Kotor", "Margin %",
                "Marketing Code", "Marketing Name" 
            };
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Sales sales : salesList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sales.getId());
                row.createCell(1).setCellValue(sales.getDocDate() != null ? sales.getDocDate().toString() : "");
                row.createCell(2).setCellValue(sales.getDocNo());
                row.createCell(3).setCellValue(sales.getCode());
                row.createCell(4).setCellValue(sales.getParName());
                row.createCell(5).setCellValue(sales.getDepCode());
                row.createCell(6).setCellValue(sales.getItemName());
                row.createCell(7).setCellValue(sales.getQty() != null ? sales.getQty() : 0);
                row.createCell(8).setCellValue(sales.getPrice() != null ? sales.getPrice().doubleValue() : 0.0);
                row.createCell(9).setCellValue(sales.getGrandTotal() != null ? sales.getGrandTotal().doubleValue() : 0.0);
                row.createCell(10).setCellValue(sales.getHppSatuan() != null ? sales.getHppSatuan().doubleValue() : 0.0);
                row.createCell(11).setCellValue(sales.getTotalHpp() != null ? sales.getTotalHpp().doubleValue() : 0.0);
                row.createCell(12).setCellValue(sales.getLabaKotor() != null ? sales.getLabaKotor().doubleValue() : 0.0);
                row.createCell(13).setCellValue(marginPercent(sales));
                row.createCell(14).setCellValue(sales.getEmpCode());
                row.createCell(15).setCellValue(sales.getEmpName());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** Margin % = laba kotor / total penjualan * 100 (2 desimal, hindari pembagian nol). */
    private double marginPercent(Sales sales) {
        if (sales.getGrandTotal() == null
                || sales.getGrandTotal().doubleValue() == 0.0
                || sales.getLabaKotor() == null) {
            return 0.0;
        }
        return Math.round(sales.getLabaKotor().doubleValue() * 100.0
                / sales.getGrandTotal().doubleValue() * 100.0) / 100.0;
    }

    public byte[] exportPurchaseToExcel(List<Purchase> purchaseList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Purchase Report");

            // Header Row - All columns matching the UI display
            Row headerRow = sheet.createRow(0);
            String[] columns = { 
                "ID", "Tanggal", "No Nota", "Kode Barang", "Distributor", 
                "Dept", "Barang", "Qty", "Harga", "Total" 
            };
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Purchase purchase : purchaseList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(purchase.getId());
                row.createCell(1).setCellValue(purchase.getDocDate() != null ? purchase.getDocDate().toString() : "");
                row.createCell(2).setCellValue(purchase.getDocNoP());
                row.createCell(3).setCellValue(purchase.getItemCode());
                row.createCell(4).setCellValue(purchase.getParName());
                row.createCell(5).setCellValue(purchase.getDepCode());
                row.createCell(6).setCellValue(purchase.getItemName());
                row.createCell(7).setCellValue(purchase.getQty() != null ? purchase.getQty() : 0);
                row.createCell(8).setCellValue(purchase.getPrice() != null ? purchase.getPrice().doubleValue() : 0.0);
                row.createCell(9).setCellValue(purchase.getGrandTotal() != null ? purchase.getGrandTotal().doubleValue() : 0.0);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}