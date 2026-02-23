package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Sales;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.SalesSummaryResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    @Autowired
    private SalesService salesService;

    // GET /api/sales?startDate=...&empCode=...&search=...
    @GetMapping
    public ResponseEntity<WebResponse<SalesSummaryResponse<Sales>>> getAllSales(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "docDate") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            // Filter Tambahan:
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "empCode", required = false) String empCode, // Filter Karyawan
            @RequestParam(name = "search", required = false) String search
    ) {
        
        SalesSummaryResponse<Sales> data = salesService.getSales(
                page, size, sortBy, direction, startDate, endDate, empCode, search
        );
        if (data.getContent().isEmpty() && data.getTotalElements() > 0 && page > 0) {
            data = salesService.getSales(0, size, sortBy, direction, startDate, endDate, empCode, search);
            page = 0;
        }

        PagingResponse paging = PagingResponse.builder()
                .currentPage(page)
                .totalPage(data.getTotalPages())
                .size(size)
                .totalItem(data.getTotalElements())
                .build();

        return ResponseEntity.ok(WebResponse.<SalesSummaryResponse<Sales>>builder()
                .status(200)
                .message("Success Fetch Sales Data")
                .data(data)
                .paging(paging)
                .build());
    }

    /** GET /api/v1/sales/employee-codes – daftar kode karyawan unik untuk dropdown filter (satu request). */
    @GetMapping("/employee-codes")
    public ResponseEntity<WebResponse<java.util.List<String>>> getEmployeeCodes() {
        java.util.List<String> codes = salesService.getEmployeeCodes();
        return ResponseEntity.ok(WebResponse.<java.util.List<String>>builder()
                .status(200)
                .message("Success fetch employee codes")
                .data(codes)
                .paging(null)
                .build());
    }
}