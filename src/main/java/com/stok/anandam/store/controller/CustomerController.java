package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Customer;
import com.stok.anandam.store.core.postgres.model.PelangganMybiz;
import com.stok.anandam.store.core.postgres.repository.CustomerRepository;
import com.stok.anandam.store.core.postgres.repository.PelangganMybizRepository;
import com.stok.anandam.store.dto.CustomerOptionResponse;
import com.stok.anandam.store.dto.WebResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final PelangganMybizRepository pelangganMybizRepository;

    @GetMapping("/options")
    public ResponseEntity<WebResponse<List<CustomerOptionResponse>>> getOptions(
            @RequestParam(name = "search", required = false) String search
    ) {
        String query = (search == null) ? "" : search;
        
        List<CustomerOptionResponse> options = new ArrayList<>();
        
        // 1. Search local Customers
        List<Customer> customers = customerRepository.findByNamaPelangganContainingIgnoreCaseAndDeletedAtIsNull(query);
        options.addAll(customers.stream()
                .limit(20)
                .map(c -> CustomerOptionResponse.builder()
                        .id(c.getId())
                        .namaPelanggan(c.getNamaPelanggan())
                        .noHp(c.getNoHp())
                        .source("LOCAL")
                        .build())
                .collect(Collectors.toList()));
        
        // 2. Search Pelanggan MyBiz
        List<PelangganMybiz> mybizCustomers = pelangganMybizRepository.findByNamaPartnerContainingIgnoreCase(query);
        options.addAll(mybizCustomers.stream()
                .limit(20)
                .map(pm -> CustomerOptionResponse.builder()
                        .id(pm.getId())
                        .namaPelanggan(pm.getNamaPartner())
                        .noHp(pm.getNoTelepon())
                        .source("SPREADSHEET")
                        .kodePartner(pm.getKodePartner())
                        .kodeMarketing(pm.getKodeMarketing())
                        .namaMarketing(pm.getNamaMarketing())
                        .limitPiutang(pm.getLimitPiutang())
                        .terminPiutang(pm.getTerminPiutang())
                        .limitHutang(pm.getLimitHutang())
                        .terminHutang(pm.getTerminHutang())
                        .npwp(pm.getNpwp())
                        .alamat(pm.getAlamat())
                        .build())
                .collect(Collectors.toList()));

        // Limit overall results
        List<CustomerOptionResponse> limitedOptions = options.stream()
                .limit(40)
                .collect(Collectors.toList());

        return ResponseEntity.ok(WebResponse.<List<CustomerOptionResponse>>builder()
                .status(200)
                .message("Success fetch customer options")
                .data(limitedOptions)
                .build());
    }
}
