package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemSerialNumberResponse {
    private LocalDate tanggal;
    private String docId;
    private String user;
    private String itemName;
    private String sn;
}