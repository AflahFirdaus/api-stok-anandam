package com.stok.anandam.store.core.postgres.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MemoStatusConverter implements AttributeConverter<MemoStatus, String> {

    @Override
    public String convertToDatabaseColumn(MemoStatus status) {
        if (status == null) return null;
        return status.name();
    }

    @Override
    public MemoStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return MemoStatus.fromName(dbData);
    }
}
