package com.stok.anandam.store.service;

import com.stok.anandam.store.core.postgres.model.RequestDelivery;
import com.stok.anandam.store.core.postgres.model.User;
import com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus;
import com.stok.anandam.store.core.postgres.repository.RequestDeliveryRepository;
import com.stok.anandam.store.core.postgres.repository.UserRepository;
import com.stok.anandam.store.dto.RequestDeliveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.stok.anandam.store.core.postgres.repository.PenjadwalanKonfirmasiRepository;
import com.stok.anandam.store.core.postgres.model.PenjadwalanKonfirmasi;

@Service
@RequiredArgsConstructor
public class RequestDeliveryService {

    private final RequestDeliveryRepository requestDeliveryRepository;
    private final UserRepository userRepository;
    private final PenjadwalanKonfirmasiRepository penjadwalanRepo;

    @Transactional
    public RequestDeliveryResponse createRequest(RequestDelivery request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        request.setCreator(creator);
        request.setStatus(RequestDeliveryStatus.MENUNGGU_GUDANG);
        request.setNomorRequest(generateNomorRequest());
        if (request.getIsUrgen() == null) request.setIsUrgen(false);

        RequestDelivery saved = requestDeliveryRepository.save(request);
        return mapToResponse(saved);
    }

    private String generateNomorRequest() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = "REQ-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        long count = requestDeliveryRepository.findTopByOrderByCreatedAtDesc()
                .map(r -> {
                    if (r.getNomorRequest() != null && r.getNomorRequest().startsWith(prefix)) {
                        String suffix = r.getNomorRequest().substring(prefix.length() + 1);
                        try {
                            return Long.parseLong(suffix) + 1;
                        } catch (Exception e) {}
                    }
                    return 1L;
                })
                .orElse(1L);

        return String.format("%s-%04d", prefix, count);
    }

    @Transactional(readOnly = true)
    public List<RequestDeliveryResponse> getListRequest(RequestDeliveryStatus status, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        List<RequestDelivery> list;
        String roleName = user.getRole().name();
        
        if (roleName.startsWith("MARKETING") || "MARKETING".equals(roleName)) {
            List<com.stok.anandam.store.core.postgres.model.Role> marketingRoles = java.util.Arrays.stream(com.stok.anandam.store.core.postgres.model.Role.values())
                    .filter(r -> r.name().startsWith("MARKETING"))
                    .collect(java.util.stream.Collectors.toList());
            
            list = requestDeliveryRepository.findByCreatorRoleIn(marketingRoles);
            
            if (status != null) {
                list = list.stream()
                        .filter(r -> r.getStatus() == status)
                        .collect(java.util.stream.Collectors.toList());
            }
        } else if ("DELIVERY".equals(roleName)) {
            List<Long> assignedRdIds = penjadwalanRepo.findByPersonelIdAndDeletedAtIsNull(user.getId())
                    .stream()
                    .filter(t -> t.getRequestDelivery() != null)
                    .map(t -> t.getRequestDelivery().getId())
                    .collect(Collectors.toList());

            list = requestDeliveryRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(r -> assignedRdIds.contains(r.getId()))
                    .collect(Collectors.toList());
            
            if (status != null) {
                list = list.stream()
                        .filter(r -> r.getStatus() == status)
                        .collect(Collectors.toList());
            }
        } else if (status != null) {
            list = requestDeliveryRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = requestDeliveryRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequestDeliveryResponse getDetail(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User login tidak ditemukan"));

        RequestDelivery rd = requestDeliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request Delivery tidak ditemukan"));
        
        // Access Check for Delivery Role
        if (user.getRole() == com.stok.anandam.store.core.postgres.model.Role.DELIVERY) {
            boolean isAssigned = penjadwalanRepo.findByPersonelIdAndDeletedAtIsNull(user.getId())
                    .stream()
                    .anyMatch(t -> t.getRequestDelivery() != null && t.getRequestDelivery().getId().equals(id));
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses ke request ini karena tidak ditugaskan kepada Anda");
            }
        }

        return mapToResponse(rd);
    }

    private RequestDeliveryResponse mapToResponse(RequestDelivery rd) {
        Long penjadwalanId = rd.getPenjadwalan() != null ? rd.getPenjadwalan().getId() : null;
        if (penjadwalanId == null) {
            List<PenjadwalanKonfirmasi> existingList = penjadwalanRepo.findAllByOrderByCreatedAtDesc().stream()
                    .filter(p -> p.getRequestDelivery() != null && p.getRequestDelivery().getId().equals(rd.getId()))
                    .collect(Collectors.toList());
            if (!existingList.isEmpty()) {
                penjadwalanId = existingList.get(0).getId();
            }
        }

        return RequestDeliveryResponse.builder()
                .id(rd.getId())
                .nomorRequest(rd.getNomorRequest())
                .receiverName(rd.getReceiverName())
                .receiverPhone(rd.getReceiverPhone())
                .alamatLengkap(rd.getAlamatLengkap())
                .alamatMaps(rd.getAlamatMaps())
                .keterangan(rd.getKeterangan())
                .status(rd.getStatus())
                .creatorId(rd.getCreator() != null ? rd.getCreator().getId() : null)
                .creatorName(rd.getCreator() != null ? rd.getCreator().getNama() : "System")
                .createdAt(rd.getCreatedAt())
                .updatedAt(rd.getUpdatedAt())
                .penjadwalanId(penjadwalanId)
                .isUrgen(rd.getIsUrgen() != null && rd.getIsUrgen())
                .build();
    }
}
