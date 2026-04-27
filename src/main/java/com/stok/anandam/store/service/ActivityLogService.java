package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.ActivityLog;
import com.stok.anandam.store.core.postgres.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    /**
     * Mencatat aktivitas ke database secara otomatis menangkap IP Address.
     * @param username Nama user pelaksana
     * @param action Nama aksi (Contoh: APPROVE_MEMO, UPDATE_STOCK)
     * @param details Detail tambahan (Contoh: ID Memo: 123, Status: DISETUJUI)
     */
    public void log(String username, String action, String details) {
        ActivityLog log = new ActivityLog();
        log.setUsername(username);
        log.setAction(action);
        log.setDetails(details);
        log.setIpAddress(getClientIp());
        activityLogRepository.save(log);
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String remoteAddr = request.getHeader("X-Forwarded-For");
            if (remoteAddr == null || remoteAddr.isEmpty()) {
                remoteAddr = request.getRemoteAddr();
            }
            return remoteAddr;
        }
        return "unknown";
    }
}
