package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTrackingResponse {
    private String noServis;
    private String namaPelangganMasked;
    private String jenisBarang;
    private String merek;
    private String modelSeri;
    private String kerusakan;
    private String statusTerkini;
    private String garansi;
    private List<TimelineLog> timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineLog {
        private String status;
        private String catatan;
        private String waktuUpdate;
    }
}