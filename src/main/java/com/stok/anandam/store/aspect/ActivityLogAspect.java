package com.stok.anandam.store.aspect;

import com.stok.anandam.store.annotation.LogActivity;
import com.stok.anandam.store.core.postgres.model.ActivityLog;
import com.stok.anandam.store.core.postgres.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class ActivityLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogAspect.class);

    @Autowired
    private ActivityLogRepository logRepository;

    @Around("@annotation(logActivity)") // Cegat method yang punya anotasi ini
    public Object logActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
        
        // 1. Jalankan method aslinya dulu (Migrasi, Simpan, dll)
        Object result;
        String status = "SUCCESS";
        String errorDetail = "";
        
        try {
            result = joinPoint.proceed(); // Eksekusi method asli
        } catch (Throwable e) {
            status = "FAILED";
            errorDetail = "Error: " + e.getMessage();
            throw e; // Lempar errornya biar tetap ketahuan Controller
        } finally {
            // 2. Apapun yang terjadi (Sukses/Gagal), Catat Log!
            saveLog(logActivity.value(), status, errorDetail);
        }

        return result;
    }

    private void saveLog(String actionName, String status, String errorDetail) {
        try {
            // Ambil User yang sedang Login
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : "SYSTEM/ANONYMOUS";

            // Ambil IP Address
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ip = request.getRemoteAddr();

            ActivityLog activityLog = new ActivityLog();
            activityLog.setUsername(username);
            activityLog.setAction(actionName);
            activityLog.setIpAddress(ip);
            activityLog.setDetails(status + ". " + errorDetail);

            logRepository.save(activityLog);
            
        } catch (Exception e) {
            log.warn("Gagal menyimpan log aktivitas: {}", e.getMessage());
        }
    }
}