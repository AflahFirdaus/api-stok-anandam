package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.AuditLogResponse;
import com.stok.anandam.store.service.AuditTrailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditTrailService auditTrailService;

    @GetMapping("/transaksi/{transaksiId}")
    public ResponseEntity<List<AuditLogResponse>> getLogs(@PathVariable String transaksiId) {
        return ResponseEntity.ok(auditTrailService.getLogsByEntityId(transaksiId));
    }
}