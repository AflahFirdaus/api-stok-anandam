package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.DashboardResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<WebResponse<DashboardResponse>> getSummary() {
        DashboardResponse data = dashboardService.getDashboardData();
        
        return ResponseEntity.ok(WebResponse.<DashboardResponse>builder()
                .status(200)
                .message("Success Fetch Dashboard Summary")
                .data(data)
                .build());
    }
}