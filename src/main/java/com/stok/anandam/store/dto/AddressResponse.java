package com.stok.anandam.store.dto;

public record AddressResponse(
    String name,
    String fullAddress,
    String postalCode,
    Double latitude,   // Penanda di Maps (Y)
    Double longitude   // Penanda di Maps (X)
) {}