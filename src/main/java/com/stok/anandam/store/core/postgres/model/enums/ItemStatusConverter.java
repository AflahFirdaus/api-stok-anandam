package com.stok.anandam.store.core.postgres.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ItemStatusConverter implements AttributeConverter<ItemStatus, String> {

    @Override
    public String convertToDatabaseColumn(ItemStatus status) {
        if (status == null) return null;
        return status.name();
    }

    @Override
    public ItemStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return ItemStatus.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}