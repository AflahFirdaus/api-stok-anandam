package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Kodepos;
import com.stok.anandam.store.core.postgres.repository.KodeposRepository;
import com.stok.anandam.store.dto.WebResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kodepos")
@RequiredArgsConstructor
public class KodeposController {

    private final KodeposRepository kodeposRepository;

    @GetMapping
    public WebResponse<List<Kodepos>> search(@RequestParam("search") String search) {
        List<Kodepos> results = kodeposRepository.findByKodePosContainingOrDesaKelurahanContainingIgnoreCase(search, search);
        return WebResponse.<List<Kodepos>>builder()
                .status(HttpStatus.OK.value())
                .message("Success search kodepos")
                .data(results)
                .build();
    }
}
