package com.stok.anandam.store.dto;

import com.stok.anandam.store.core.postgres.model.enums.ItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemStatusRequest {
    @NotNull(message = "Item status tidak boleh kosong")
    private ItemStatus itemStatus;
}