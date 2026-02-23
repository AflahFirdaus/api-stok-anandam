package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.ActivityLog;
import com.stok.anandam.store.core.postgres.repository.ActivityLogRepository;
import com.stok.anandam.store.dto.ActivityLogResponse;
import com.stok.anandam.store.dto.PagingResponse;
import com.stok.anandam.store.dto.WebResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/activity-logs")
public class ActivityLogController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "timestamp", "username", "action", "details", "ipAddress");

    @Autowired
    private ActivityLogRepository activityLogRepository;

    //  GET /api/v1/activity-logs
    @GetMapping
    public ResponseEntity<WebResponse<List<ActivityLogResponse>>> getAllActivityLogs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "timestamp") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "action", required = false) String action
    ) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "timestamp";
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, safeSortBy));

        Page<ActivityLog> logsPage;

        // Filter: bisa kombinasi username DAN action, atau salah satu saja
        if (username != null && !username.isBlank() && action != null && !action.isBlank()) {
            // Filter kombinasi: username DAN action
            logsPage = activityLogRepository.findByUsernameContainingIgnoreCaseAndActionContainingIgnoreCase(
                    username, action, pageable);
        } else if (username != null && !username.isBlank()) {
            // Filter hanya username
            logsPage = activityLogRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else if (action != null && !action.isBlank()) {
            // Filter hanya action
            logsPage = activityLogRepository.findByActionContainingIgnoreCase(action, pageable);
        } else {
            // Tidak ada filter, ambil semua
            logsPage = activityLogRepository.findAll(pageable);
        }

        if (logsPage.getTotalPages() > 0 && page >= logsPage.getTotalPages()) {
            pageable = PageRequest.of(0, size, Sort.by(sortDirection, safeSortBy));
            if (username != null && !username.isBlank() && action != null && !action.isBlank()) {
                logsPage = activityLogRepository.findByUsernameContainingIgnoreCaseAndActionContainingIgnoreCase(username, action, pageable);
            } else if (username != null && !username.isBlank()) {
                logsPage = activityLogRepository.findByUsernameContainingIgnoreCase(username, pageable);
            } else if (action != null && !action.isBlank()) {
                logsPage = activityLogRepository.findByActionContainingIgnoreCase(action, pageable);
            } else {
                logsPage = activityLogRepository.findAll(pageable);
            }
            page = 0;
        }

        // Convert ke DTO
        List<ActivityLogResponse> responses = logsPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Paging metadata
        PagingResponse pagingResponse = PagingResponse.builder()
                .currentPage(page)
                .totalPage(logsPage.getTotalPages())
                .size(size)
                .totalItem(logsPage.getTotalElements())
                .build();

        WebResponse<List<ActivityLogResponse>> response = WebResponse.<List<ActivityLogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Success fetch activity logs")
                .data(responses)
                .paging(pagingResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/activity-logs/{id}
     * Detail activity log berdasarkan ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<WebResponse<ActivityLogResponse>> getActivityLogById(@PathVariable Long id) {
        ActivityLog log = activityLogRepository.findById(id)
                .orElseThrow(() -> new com.stok.anandam.store.exception.ResourceNotFoundException(
                        "Activity log dengan ID " + id + " tidak ditemukan"));

        WebResponse<ActivityLogResponse> response = WebResponse.<ActivityLogResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Success fetch activity log")
                .data(toResponse(log))
                .paging(null)
                .build();

        return ResponseEntity.ok(response);
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .action(log.getAction())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .build();
    }
}
