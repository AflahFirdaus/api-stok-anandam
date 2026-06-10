package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

public enum StatusPembayaran {
    LUNAS("Lunas"),
    BELUM_LUNAS("Belum Lunas"),
    JATUH_TEMPO("Jatuh Tempo");

    private final String value;

    StatusPembayaran(String value) { 
        this.value = value; 
    }

    public String getValue() { 
        return value; 
    }

    @Converter(autoApply = true)
    public static class MappedConverter implements AttributeConverter<StatusPembayaran, String> {
        @Override
        public String convertToDatabaseColumn(StatusPembayaran attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public StatusPembayaran convertToEntityAttribute(String dbData) {
            if (dbData == null) return null;
            return Stream.of(StatusPembayaran.values())
                    .filter(c -> c.getValue().equals(dbData))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }
}