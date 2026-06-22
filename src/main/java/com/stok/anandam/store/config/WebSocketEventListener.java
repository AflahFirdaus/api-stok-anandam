package com.stok.anandam.store.config;

import com.stok.anandam.store.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserSessionService userSessionService;

    public WebSocketEventListener(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Ambil header custom yang dikirim dari Flutter saat koneksi
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        // Ambil userId dan name dari header nativeMessage
        String userId = null;
        String name = null;

        // Coba ambil dari header kustom
        if (event.getMessage() != null) {
            userId = headerAccessor.getFirstNativeHeader("userId");
            name = headerAccessor.getFirstNativeHeader("name");
        }

        if (userId != null) {
            // Simpan userId dan name ke session attributes agar bisa dipakai saat disconnect
            if (sessionAttributes != null) {
                sessionAttributes.put("userId", userId);
                sessionAttributes.put("name", name);
            }

            // Simpan session ke Redis
            userSessionService.saveUserSession(userId, name, "connected");
            log.info("WebSocket connected: userId={}, name={}, sessionId={}",
                    userId, name, headerAccessor.getSessionId());
        } else {
            log.warn("WebSocket connect event without userId header, sessionId={}",
                    headerAccessor.getSessionId());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Ambil userId dari session attributes yang disimpan saat connect
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String userId = null;

        if (sessionAttributes != null) {
            userId = (String) sessionAttributes.get("userId");
        }

        if (userId != null) {
            // Opsi 1: Hapus key langsung (default - untuk presence akurat)
            userSessionService.removeUserSession(userId);

            // Opsi 2 (alternatif): Set status offline (uncomment jika ingin menyimpan riwayat)
            // userSessionService.setUserOffline(userId);

            log.info("WebSocket disconnected: userId={}, sessionId={}",
                    userId, headerAccessor.getSessionId());
        } else {
            log.warn("WebSocket disconnect event without userId in session, sessionId={}",
                    headerAccessor.getSessionId());
        }
    }
}