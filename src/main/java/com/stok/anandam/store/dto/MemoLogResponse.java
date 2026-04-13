package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MemoLogResponse {
    private String status;
    private Long aktorId;
    private String keterangan;
    private LocalDateTime createdAt;
}