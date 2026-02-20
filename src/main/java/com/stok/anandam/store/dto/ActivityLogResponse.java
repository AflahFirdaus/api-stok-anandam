package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityLogResponse {
    private Long id;
    private String username;
    private String action;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
}
