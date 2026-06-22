package com.stok.anandam.store.controller;

import com.stok.anandam.store.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class UserActivityController {

    private static final Logger log = LoggerFactory.getLogger(UserActivityController.class);

    private final UserSessionService userSessionService;

    public UserActivityController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    /**
     * Menerima payload aktivitas user dari Flutter.
     * Flutter mengirim ke /app/user-action dengan format JSON:
     * {
     *   "userId": "123",
     *   "currentAction": "Halaman Penjualan",
     *   "metadata": { ... } (opsional)
     * }
     */
    @MessageMapping("/user-action")
    public void handleUserAction(
            @Payload Map<String, Object> payload,
            @Header("simpSessionId") String sessionId) {

        String userId = payload != null ? (String) payload.get("userId") : null;
        String currentAction = payload != null ? (String) payload.get("currentAction") : null;
        String name = payload != null ? (String) payload.get("name") : null;

        if (userId == null || userId.isEmpty()) {
            log.warn("Received user-action without userId from session {}", sessionId);
            return;
        }

        if (currentAction == null) {
            currentAction = "idle";
        }

        // Update currentAction di Redis dan reset TTL
        // Jika session sudah expired (idle > 5 menit), akan otomatis re-create
        userSessionService.updateUserAction(userId, name, currentAction);

        log.debug("User action received: userId={}, action={}, sessionId={}",
                userId, currentAction, sessionId);
    }
}