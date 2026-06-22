package com.stok.anandam.store.service;

import com.stok.anandam.store.dto.UserSessionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableScheduling
public class AdminBroadcasterService {

    private static final Logger log = LoggerFactory.getLogger(AdminBroadcasterService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionService userSessionService;

    public AdminBroadcasterService(SimpMessagingTemplate messagingTemplate,
                                   UserSessionService userSessionService) {
        this.messagingTemplate = messagingTemplate;
        this.userSessionService = userSessionService;
    }

    /**
     * Scheduler yang berjalan setiap 2 detik.
     * Mengambil seluruh data dari keys user_session:* di Redis,
     * lalu broadcast array of objects ke /topic/admin-dashboard.
     */
    @Scheduled(fixedRate = 2000) // 2 detik
    public void broadcastActiveSessions() {
        try {
            // Ambil semua session user dari Redis
            List<UserSessionDto> sessions = userSessionService.getAllActiveSessions();

            // Broadcast ke semua subscriber /topic/admin-dashboard
            messagingTemplate.convertAndSend("/topic/admin-dashboard", sessions);

            log.trace("Broadcasted {} active sessions to /topic/admin-dashboard", sessions.size());
        } catch (Exception e) {
            log.error("Error broadcasting active sessions: {}", e.getMessage());
        }
    }
}