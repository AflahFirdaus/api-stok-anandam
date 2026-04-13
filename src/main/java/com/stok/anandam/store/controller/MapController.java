package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.MapDeliveryResponse;
import com.stok.anandam.store.dto.WebResponse;
import com.stok.anandam.store.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping("/pengiriman")
    public WebResponse<List<MapDeliveryResponse>> getTodaysDeliveries(java.security.Principal principal) {
        List<MapDeliveryResponse> data = mapService.getTodaysDeliveries(principal.getName());
        return WebResponse.<List<MapDeliveryResponse>>builder()
                .data(data)
                .message("Successfully fetched today's deliveries with coordinates")
                .build();
    }
}
