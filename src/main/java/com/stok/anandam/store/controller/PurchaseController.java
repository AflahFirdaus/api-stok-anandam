package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Purchase;
import com.stok.anandam.store.dto.PurchaseSummaryResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    // GET /api/purchases?startDate=2023-01-01&endDate=2023-01-31&search=Budi
    @GetMapping
    public ResponseEntity<WebResponse<PurchaseSummaryResponse<Purchase>>> getPurchases(

            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "docDate") String sortBy,
            @RequestParam(name = "dir", defaultValue = "desc") String dir,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "search", required = false) String search
    ) {
        
        PurchaseSummaryResponse<Purchase> data = purchaseService.getPurchases(
                page, size, sortBy, dir, startDate, endDate, search
        );

        return ResponseEntity.ok(WebResponse.<PurchaseSummaryResponse<Purchase>>builder()
                .status(200)
                .message("Success fetch purchase data")
                .data(data)
                .build());
    }
}