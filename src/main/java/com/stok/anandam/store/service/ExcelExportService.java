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

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = { "ID", "Date", "Doc No", "Partner Name", "Item Name", "Qty", "Price", "Grand Total",
                    "Emp Name" };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Sales sales : salesList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sales.getId());
                row.createCell(1).setCellValue(sales.getDocDate() != null ? sales.getDocDate().toString() : "");
                row.createCell(2).setCellValue(sales.getDocNo());
                row.createCell(3).setCellValue(sales.getParName());
                row.createCell(4).setCellValue(sales.getItemName());
                row.createCell(5).setCellValue(sales.getQty() != null ? sales.getQty() : 0);
                row.createCell(6).setCellValue(sales.getPrice() != null ? sales.getPrice().doubleValue() : 0.0);
                row.createCell(7)
                        .setCellValue(sales.getGrandTotal() != null ? sales.getGrandTotal().doubleValue() : 0.0);
                row.createCell(8).setCellValue(sales.getEmpName());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportPurchaseToExcel(List<Purchase> purchaseList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Purchase Report");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = { "ID", "Date", "Doc No", "Partner Name", "Item Name", "Qty", "Price", "Grand Total" };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Purchase purchase : purchaseList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(purchase.getId());
                row.createCell(1).setCellValue(purchase.getDocDate() != null ? purchase.getDocDate().toString() : "");
                row.createCell(2).setCellValue(purchase.getDocNoP());
                row.createCell(3).setCellValue(purchase.getParName());
                row.createCell(4).setCellValue(purchase.getItemName());
                row.createCell(5).setCellValue(purchase.getQty() != null ? purchase.getQty() : 0);
                row.createCell(6).setCellValue(purchase.getPrice() != null ? purchase.getPrice().doubleValue() : 0.0);
                row.createCell(7)
                        .setCellValue(purchase.getGrandTotal() != null ? purchase.getGrandTotal().doubleValue() : 0.0);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
