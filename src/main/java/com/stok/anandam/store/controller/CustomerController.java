package com.stok.anandam.store.controller;

import com.stok.anandam.store.core.postgres.model.Customer;
import com.stok.anandam.store.core.postgres.repository.CustomerRepository;
import com.stok.anandam.store.dto.CustomerOptionResponse;
import com.stok.anandam.store.dto.WebResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;

    @GetMapping("/options")
    public ResponseEntity<WebResponse<List<CustomerOptionResponse>>> getOptions(
            @RequestParam(name = "search", required = false) String search
    ) {
        String query = (search == null) ? "" : search;
        List<Customer> customers = customerRepository.findByNamaPelangganContainingIgnoreCaseAndDeletedAtIsNull(query);
        
        // Limit results for autocomplete performance
        List<CustomerOptionResponse> options = customers.stream()
                .limit(20)
                .map(c -> CustomerOptionResponse.builder()
                        .id(c.getId())
                        .namaPelanggan(c.getNamaPelanggan())
                        .noHp(c.getNoHp())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(WebResponse.<List<CustomerOptionResponse>>builder()
                .status(200)
                .message("Success fetch customer options")
                .data(options)
                .build());
    }
}
