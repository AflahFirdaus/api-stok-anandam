package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.model.Role;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.core.postgres.model.enums.MemoStatus;
import com.stok.anandam.store.core.postgres.repository.KodeposRepository;
import com.stok.anandam.store.core.postgres.repository.MemoRepository;
import com.stok.anandam.store.core.postgres.repository.PenjadwalanKonfirmasiRepository;
import com.stok.anandam.store.core.postgres.repository.RequestDeliveryRepository;
import com.stok.anandam.store.dto.MapDeliveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final PenjadwalanKonfirmasiRepository penjadwalanRepository;
    private final MemoRepository memoRepository;
    private final KodeposRepository kodeposRepository;
    private final UserRepository userRepository;
    private final RequestDeliveryRepository requestDeliveryRepository;

    public List<MapDeliveryResponse> getTodaysDeliveries(String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isDelivery = currentUser.getRole() == Role.DELIVERY;
        List<MapDeliveryResponse> results = new ArrayList<>();

        // 1. Fetch from MEMO (Normal Tasks)
        results.addAll(memoRepository.findAll().stream()
                .filter(m -> m.getIsDeliveryRequired() != null && m.getIsDeliveryRequired())
                .filter(m -> m.getStatusAkhir() != MemoStatus.DRAFT 
                        && m.getStatusAkhir() != MemoStatus.SELESAI 
                        && m.getStatusAkhir() != MemoStatus.DIBATALKAN)
                .map(memo -> {
                    var schedule = penjadwalanRepository.findByMemo_IdAndDeletedAtIsNull(memo.getId())
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (isDelivery) {
                        if (schedule == null || schedule.getPersonelId() == null || !schedule.getPersonelId().equals(currentUser.getId())) {
                            return null;
                        }
                    }

                    MapDeliveryResponse.MapDeliveryResponseBuilder builder = MapDeliveryResponse.builder()
                            .idMemo(memo.getId().toString())
                            .nomorMemo(memo.getNomorMemo())
                            .customerName(memo.getCustomer() != null ? memo.getCustomer().getNamaPelanggan() : "N/A")
                            .memoStatus(memo.getStatusAkhir().name())
                            .senderName(memo.getMarketingName() != null ? memo.getMarketingName() : "Admin")
                            .isUrgen(schedule != null && schedule.getIsUrgen() != null && schedule.getIsUrgen())
                            .isManual(false)
                            .isExpedition(schedule != null && schedule.getTipeTugas() == com.stok.anandam.store.core.postgres.model.enums.TipeTugas.DROP_OFF_EKSPEDISI);

                    if (schedule != null) {
                        builder.status(schedule.getStatusJadwal().name())
                                .desa(schedule.getKodepos() != null ? schedule.getKodepos().getDesaKelurahan() : "N/A")
                                .kecamatan(schedule.getKodepos() != null ? schedule.getKodepos().getKecamatan() : "N/A")
                                .kabupaten(schedule.getKodepos() != null ? schedule.getKodepos().getKabupatenKota() : "N/A")
                                .kodePos(schedule.getKodepos() != null ? schedule.getKodepos().getKodePos() : memo.getKodePos())
                                .lat(schedule.getKodepos() != null ? schedule.getKodepos().getLatitude() : null)
                                .lng(schedule.getKodepos() != null ? schedule.getKodepos().getLongitude() : null)
                                .mapUrl(schedule.getAlamatMaps());
                                
                        if (schedule.getMarketingName() != null) {
                            builder.senderName(schedule.getMarketingName());
                        }
                    }

                    if (builder.build().getLat() == null && (memo.getKodePos() != null || (schedule != null && schedule.getAlamatMaps() != null))) {
                        // Try from URL first
                        if (schedule != null && schedule.getAlamatMaps() != null) {
                            var coords = extractCoords(schedule.getAlamatMaps());
                            if (coords != null) {
                                builder.lat(coords[0]).lng(coords[1]);
                            }
                        }
                        
                        // If still null, try from Kodepos
                        if (builder.build().getLat() == null && memo.getKodePos() != null) {
                             kodeposRepository.findFirstByKodePos(memo.getKodePos()).ifPresent(kp -> {
                                builder.desa(kp.getDesaKelurahan())
                                        .kecamatan(kp.getKecamatan())
                                        .kabupaten(kp.getKabupatenKota())
                                        .kodePos(kp.getKodePos())
                                        .lat(kp.getLatitude())
                                        .lng(kp.getLongitude());
                            });
                        }
                        
                        if (builder.build().getStatus() == null) {
                            builder.status("BELUM_DIJADWALKAN");
                        }
                    }

                    return builder.build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList()));

        // 2. Fetch from MANUAL REQUESTS (Non-Memo Tasks)
        results.addAll(penjadwalanRepository.findByMemoIsNullAndDeletedAtIsNull().stream()
                .map(manual -> {
                    if (isDelivery) {
                        if (manual.getPersonelId() == null || !manual.getPersonelId().equals(currentUser.getId())) {
                            return null;
                        }
                    }

                    // Fallback hierarchy for Customer Name
                    String name = manual.getManualCustomerName();
                    if (name == null || name.isBlank()) {
                        if (manual.getRequestDelivery() != null) {
                            name = manual.getRequestDelivery().getReceiverName();
                        }
                    }
                    if (name == null || name.isBlank()) name = "No Name";

                    // Fallback hierarchy for Sender (Creator)
                    String sender = manual.getMarketingName();
                    if (sender == null || sender.isBlank()) {
                        if (manual.getRequestDelivery() != null && manual.getRequestDelivery().getCreator() != null) {
                            sender = manual.getRequestDelivery().getCreator().getNama();
                        }
                    }
                    if (sender == null || sender.isBlank()) sender = "Manual Request";

                    // Fallback hierarchy for Address components
                    String desa = manual.getKodepos() != null ? manual.getKodepos().getDesaKelurahan() : null;
                    String kec = manual.getKodepos() != null ? manual.getKodepos().getKecamatan() : null;
                    String kab = manual.getKodepos() != null ? manual.getKodepos().getKabupatenKota() : null;
                    
                    if (kec == null && manual.getAlamatLengkap() != null) {
                        try {
                            String[] parts = manual.getAlamatLengkap().split(",");
                            if (parts.length >= 2) {
                                kec = parts[parts.length > 2 ? parts.length - 2 : 0].trim();
                                if (parts.length >= 3) kab = parts[parts.length - 1].trim();
                            }
                        } catch (Exception ignored) {}
                    }

                    MapDeliveryResponse.MapDeliveryResponseBuilder builder = MapDeliveryResponse.builder()
                            .idMemo(null)
                            .nomorMemo(manual.getRequestDelivery() != null ? manual.getRequestDelivery().getNomorRequest() : "MANUAL-" + manual.getId())
                            .customerName(name)
                            .senderName(sender)
                            .memoStatus("MANUAL")
                            .isUrgen(manual.getIsUrgen() != null && manual.getIsUrgen())
                            .isManual(true)
                            .isExpedition(manual.getTipeTugas() == com.stok.anandam.store.core.postgres.model.enums.TipeTugas.DROP_OFF_EKSPEDISI)
                            .status(manual.getStatusJadwal().name())
                            .mapUrl(manual.getAlamatMaps())
                            .desa(desa != null ? desa : "Wilayah")
                            .kecamatan(kec != null ? kec : "N/A")
                            .kabupaten(kab != null ? kab : "N/A")
                            .kodePos(manual.getKodepos() != null ? manual.getKodepos().getKodePos() : "N/A")
                            .lat(manual.getKodepos() != null ? manual.getKodepos().getLatitude() : null)
                            .lng(manual.getKodepos() != null ? manual.getKodepos().getLongitude() : null);

                    // Robust coordinate extraction from URL/String
                    if (builder.build().getLat() == null) {
                         String source = manual.getAlamatMaps();
                         if (source == null && manual.getRequestDelivery() != null) source = manual.getRequestDelivery().getAlamatMaps();
                         
                         var coords = extractCoords(source);
                         if (coords != null) {
                             builder.lat(coords[0]).lng(coords[1]);
                         }
                    }

                    // Fallback to Postal Code Coordinates if URL failed
                    if (builder.build().getLat() == null && manual.getRequestDelivery() != null && manual.getRequestDelivery().getKodePos() != null) {
                         kodeposRepository.findFirstByKodePos(manual.getRequestDelivery().getKodePos()).ifPresent(kp -> {
                             builder.lat(kp.getLatitude())
                                    .lng(kp.getLongitude());
                             if (builder.build().getDesa() == null || builder.build().getDesa().equals("Wilayah")) {
                                 builder.desa(kp.getDesaKelurahan())
                                        .kecamatan(kp.getKecamatan())
                                        .kabupaten(kp.getKabupatenKota());
                             }
                         });
                    }

                    return builder.build();
                })
                .filter(java.util.Objects::nonNull)
                 .collect(Collectors.toList()));

        // 3. Fetch from UNSCHEDULED Request Deliveries
        if (!isDelivery) {
            List<Long> scheduledRdIds = penjadwalanRepository.findByMemoIsNullAndDeletedAtIsNull().stream()
                    .filter(p -> p.getRequestDelivery() != null)
                    .map(p -> p.getRequestDelivery().getId())
                    .collect(Collectors.toList());

            results.addAll(requestDeliveryRepository.findAll().stream()
                    .filter(rd -> rd.getStatus() != com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.SELESAI 
                            && rd.getStatus() != com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus.DIBATALKAN)
                    .filter(rd -> !scheduledRdIds.contains(rd.getId()))
                    .map(rd -> {
                        String sender = rd.getCreator() != null ? rd.getCreator().getNama() : "Manual Request";
                        
                        MapDeliveryResponse.MapDeliveryResponseBuilder builder = MapDeliveryResponse.builder()
                                .idMemo(null)
                                .nomorMemo(rd.getNomorRequest())
                                .customerName(rd.getReceiverName())
                                .senderName(sender)
                                .memoStatus("MANUAL")
                                .isUrgen(rd.getIsUrgen() != null && rd.getIsUrgen())
                                .isManual(true)
                                .isExpedition(false) // Not set until scheduled
                                .status("BELUM_DIJADWALKAN")
                                .mapUrl(rd.getAlamatMaps())
                                .kodePos(rd.getKodePos() != null ? rd.getKodePos() : "N/A");

                        // Robust coordinate extraction from URL/String
                        var coords = extractCoords(rd.getAlamatMaps());
                        if (coords != null) {
                            builder.lat(coords[0]).lng(coords[1]);
                        }

                        // Fallback to Postal Code Coordinates
                        if (builder.build().getLat() == null && rd.getKodePos() != null) {
                             kodeposRepository.findFirstByKodePos(rd.getKodePos()).ifPresent(kp -> {
                                 builder.lat(kp.getLatitude())
                                        .lng(kp.getLongitude())
                                        .desa(kp.getDesaKelurahan())
                                        .kecamatan(kp.getKecamatan())
                                        .kabupaten(kp.getKabupatenKota());
                             });
                        }

                        if (builder.build().getDesa() == null) {
                             builder.desa("Wilayah")
                                    .kecamatan("N/A")
                                    .kabupaten("N/A");
                        }

                        return builder.build();
                    })
                    .collect(Collectors.toList()));
        }

        // Final coordinate fallback to DIY Center
        results.forEach(resp -> {
            if (resp.getLat() == null) {
                resp.setLat(new java.math.BigDecimal("-7.7956"));
                resp.setLng(new java.math.BigDecimal("110.3695"));
            }
        });

        return results;
    }

    private java.math.BigDecimal[] extractCoords(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            // 1. Pattern for lat,lng in URL or string: -7.123,110.123 or @-7.123,110.123
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("([@\\s,])(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)");
            java.util.regex.Matcher m = p.matcher(source);
            if (m.find()) {
                return new java.math.BigDecimal[]{
                    new java.math.BigDecimal(m.group(2)),
                    new java.math.BigDecimal(m.group(3))
                };
            }
            
            // 2. Simple fallback regex for cases without prefix
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("^(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)$");
            java.util.regex.Matcher m2 = p2.matcher(source.trim());
            if (m2.find()) {
                return new java.math.BigDecimal[]{
                    new java.math.BigDecimal(m2.group(1)),
                    new java.math.BigDecimal(m2.group(2))
                };
            }
            
            // Try query parameter: query=-7.123,110.123
            if (source.contains("query=")) {
                String sub = source.substring(source.indexOf("query=") + 6);
                String[] parts = sub.split(",");
                if (parts.length >= 2) {
                     return new java.math.BigDecimal[]{
                        new java.math.BigDecimal(parts[0].replaceAll("[^0-9.-]", "")),
                        new java.math.BigDecimal(parts[1].replaceAll("[^0-9.-]", ""))
                    };
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
