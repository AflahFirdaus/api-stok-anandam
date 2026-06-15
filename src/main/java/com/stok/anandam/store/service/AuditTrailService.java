package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.AuditLog;
import com.stok.anandam.store.core.postgres.repository.AuditLogRepository;
import com.stok.anandam.store.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final AuditLogRepository auditLogRepository;

    // --- Method untuk mencatat log (field-level change) ---
    public void logChange(String table, String id, String field, String oldVal, String newVal, String username) {
        if (oldVal != null && oldVal.equals(newVal)) return;

        AuditLog log = AuditLog.builder()
                .tableName(table)
                .entityId(id)
                .fieldName(field)
                .oldValue(oldVal != null ? oldVal : "NULL")
                .newValue(newVal != null ? newVal : "NULL")
                .changedBy(username)
                .build();
        
        auditLogRepository.save(log);
    }

    // --- Method untuk mencatat log dengan deskripsi lengkap (action-based, misal CREATE/UPDATE dengan konteks) ---
    public void logAction(String table, String id, String actionType, String oldVal, String newVal, String username) {
        AuditLog log = AuditLog.builder()
                .tableName(table)
                .entityId(id)
                .fieldName(actionType) // actionType seperti "CREATE_NOTA_SERVIS"
                .oldValue(oldVal != null ? oldVal : "-")
                .newValue(newVal != null ? newVal : "-")
                .changedBy(username)
                .build();
        
        auditLogRepository.save(log);
    }

    // --- Method yang Anda butuhkan di Controller ---
    public List<AuditLogResponse> getLogsByEntityId(String entityId) {
        return auditLogRepository.findByEntityIdOrderByChangedAtDesc(entityId)
                .stream()
                .map(log -> AuditLogResponse.builder()
                        .tableName(log.getTableName())
                        .fieldName(log.getFieldName())
                        .oldValue(log.getOldValue())
                        .newValue(log.getNewValue())
                        .changedBy(log.getChangedBy())
                        .changedAt(log.getChangedAt())
                        .build())
                .collect(Collectors.toList());
    }
}