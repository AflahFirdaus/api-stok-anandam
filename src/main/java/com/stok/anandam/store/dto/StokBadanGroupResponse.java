package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response grup per badan untuk endpoint stok per badan.
 * Mirip dengan StokBadanGroup / StokPerBadan di portal-tim-project.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StokBadanGroupResponse {
    private String badan;
    private Integer totalItems;
    private Integer totalQty;
    private List<StokBadanItem> items;
}