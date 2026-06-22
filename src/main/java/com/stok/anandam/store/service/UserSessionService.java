package com.stok.anandam.store.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stok.anandam.store.dto.UserSessionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class UserSessionService {

    private static final Logger log = LoggerFactory.getLogger(UserSessionService.class);
    private static final String SESSION_KEY_PREFIX = "user_session:";
    private static final long SESSION_TTL_SECONDS = 300; // 5 menit

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public UserSessionService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Menyimpan session user ke Redis dengan status "online".
     */
    public void saveUserSession(String userId, String name, String currentAction) {
        String key = SESSION_KEY_PREFIX + userId;
        String now = Instant.now().toString();

        UserSessionDto session = new UserSessionDto(
                userId,
                name,
                "online",
                currentAction != null ? currentAction : "idle",
                now
        );

        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Session saved for user {}: {}", userId, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user session for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Mengupdate currentAction user dan reset TTL menjadi 5 menit.
     */
    public void updateUserAction(String userId, String name, String currentAction) {
        String key = SESSION_KEY_PREFIX + userId;

        // Ambil data session yang sudah ada
        String existingJson = (String) redisTemplate.opsForValue().get(key);
        if (existingJson == null) {
            // Session sudah expired di Redis (TTL habis karena idle),
            // tapi user masih aktif (masih mengirim pesan via WebSocket).
            // Re-create session agar user tetap ter-track.
            log.info("Session expired for user {}, re-creating session with action: {}", userId, currentAction);
            saveUserSession(userId, name, currentAction);
            return;
        }

        try {
            UserSessionDto session = objectMapper.readValue(existingJson, UserSessionDto.class);
            session.setCurrentAction(currentAction);
            session.setLastActive(Instant.now().toString());
            session.setStatus("online");

            // Update name jika tersedia (untuk menjaga data tetap fresh)
            if (name != null && !name.isEmpty()) {
                session.setName(name);
            }

            String updatedJson = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, updatedJson, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Action updated for user {}: {}", userId, currentAction);
        } catch (JsonProcessingException e) {
            log.error("Failed to update user action for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Menghapus session user dari Redis (saat disconnect).
     */
    public void removeUserSession(String userId) {
        String key = SESSION_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Session removed for user {}", userId);
    }

    /**
     * Menandai user sebagai offline (tanpa menghapus key, menyimpan riwayat).
     */
    public void setUserOffline(String userId) {
        String key = SESSION_KEY_PREFIX + userId;
        String existingJson = (String) redisTemplate.opsForValue().get(key);

        if (existingJson == null) {
            return;
        }

        try {
            UserSessionDto session = objectMapper.readValue(existingJson, UserSessionDto.class);
            session.setStatus("offline");
            session.setLastActive(Instant.now().toString());

            String updatedJson = objectMapper.writeValueAsString(session);
            // TTL diperpanjang sedikit agar admin masih bisa melihat status offline
            redisTemplate.opsForValue().set(key, updatedJson, 60, TimeUnit.SECONDS);
            log.debug("User {} set to offline", userId);
        } catch (JsonProcessingException e) {
            log.error("Failed to set user offline for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Mengambil semua session user yang aktif dari Redis.
     */
    public List<UserSessionDto> getAllActiveSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
        List<UserSessionDto> sessions = new ArrayList<>();

        if (keys == null || keys.isEmpty()) {
            return sessions;
        }

        for (String key : keys) {
            String json = (String) redisTemplate.opsForValue().get(key);
            if (json != null) {
                try {
                    UserSessionDto session = objectMapper.readValue(json, UserSessionDto.class);
                    sessions.add(session);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize session for key {}: {}", key, e.getMessage());
                }
            }
        }

        return sessions;
    }
}