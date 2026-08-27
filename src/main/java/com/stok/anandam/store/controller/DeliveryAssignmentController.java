package com.stok.anandam.store.controller;

import com.stok.anandam.store.annotation.LogActivity;
import com.stok.anandam.store.dto.DeliveryScanRequest;
import com.stok.anandam.store.dto.DeliveryScanResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryAssignmentController {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @LogActivity("Delivery melakukan scan QR Code untuk mengambil tugas pengiriman")
    @PostMapping(
            path = "/scan",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<DeliveryScanResponse> scanAndAssign(
            @RequestBody @Valid DeliveryScanRequest request,
            java.security.Principal principal) {
        return deliveryAssignmentService.scanAndAssign(request.getQrCode(), principal.getName());
    }

    @LogActivity("Delivery melepas tugas pengiriman (unassign)")
    @PostMapping(
            path = "/{penjadwalanId}/release",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> releaseTask(
            @PathVariable Long penjadwalanId,
            java.security.Principal principal) {
        return deliveryAssignmentService.releaseTask(penjadwalanId, principal.getName());
    }
}
