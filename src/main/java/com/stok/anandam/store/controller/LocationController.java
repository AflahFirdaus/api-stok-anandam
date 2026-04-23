package com.stok.anandam.store.controller;

import com.stok.anandam.store.dto.AddressResponse;
import com.stok.anandam.store.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<AddressResponse>> search(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<AddressResponse> results = locationService.searchAddress(q);
        return ResponseEntity.ok(results);
    }
}