package com.stok.anandam.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Topic untuk broadcast (Server to Client)
        config.enableSimpleBroker("/topic");
        // Prefix untuk pesan dari Client ke Server (jika ada)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint koneksi WebSocket untuk Presence & Activity Tracking
        registry.addEndpoint("/ws-connect")
                .setAllowedOriginPatterns("*") // Izinkan semua origin
                .withSockJS(); // Fallback jika browser tidak support WebSocket
                
        // Endpoint tanpa SockJS (untuk client tertentu seperti Dart/Flutter)
        registry.addEndpoint("/ws-connect")
                .setAllowedOriginPatterns("*");
    }
}
