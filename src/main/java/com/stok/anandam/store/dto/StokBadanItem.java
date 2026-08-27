package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Satu baris item stok untuk endpoint stok per badan.
 * Mirip dengan StokItemRow di portal-tim-project.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StokBadanItem {
    private String badan;
    private String depCode;
    private String depName;
    private String itemCode;
    private String itemName;
    private Integer stokQty;
    private Integer lineCount;
}