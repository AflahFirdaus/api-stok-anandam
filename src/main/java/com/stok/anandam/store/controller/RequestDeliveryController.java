package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.RequestDelivery;
import com.stok.anandam.store.core.postgres.model.enums.RequestDeliveryStatus;
import com.stok.anandam.store.dto.RequestDeliveryResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.RequestDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/request-delivery", "/api/v1/request-delivery/"})
@RequiredArgsConstructor
public class RequestDeliveryController {

    private final RequestDeliveryService requestDeliveryService;

    @PostMapping
    public WebResponse<RequestDeliveryResponse> createRequest(
            @RequestBody RequestDelivery request,
            java.security.Principal principal) {
        RequestDeliveryResponse response = requestDeliveryService.createRequest(request, principal.getName());
        return WebResponse.<RequestDeliveryResponse>builder()
                .status(200)
                .message("Request Delivery berhasil dibuat")
                .data(response)
                .build();
    }

    @GetMapping
    public WebResponse<List<RequestDeliveryResponse>> getListRequest(
            @RequestParam(required = false) RequestDeliveryStatus status,
            java.security.Principal principal) {
        List<RequestDeliveryResponse> list = requestDeliveryService.getListRequest(status, principal.getName());
        return WebResponse.<List<RequestDeliveryResponse>>builder()
                .status(200)
                .message("Berhasil mengambil data request delivery")
                .data(list)
                .build();
    }

    @GetMapping("/{id}")
    public WebResponse<RequestDeliveryResponse> getDetail(@PathVariable Long id, java.security.Principal principal) {
        RequestDeliveryResponse response = requestDeliveryService.getDetail(id, principal.getName());
        return WebResponse.<RequestDeliveryResponse>builder()
                .status(200)
                .message("Berhasil mengambil detail request delivery")
                .data(response)
                .build();
    }
}
