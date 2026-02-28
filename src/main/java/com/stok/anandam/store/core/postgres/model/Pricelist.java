package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "pricelist", indexes = {
        @Index(name = "idx_pricelist_item_name", columnList = "item_name")
})
public class Pricelist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pricelist_seq_gen")
    @SequenceGenerator(name = "pricelist_seq_gen", sequenceName = "pricelist_seq", allocationSize = 50)
    private Long id;

    @Column(name = "item_name", length = 500, unique = true)
    private String itemName;

    @Column(name = "spesifikasi", length = 3000)
    private String spesifikasi;

    @Column(name = "modal", precision = 19, scale = 2)
    private BigDecimal modal;

    @Column(name = "final_pricelist", precision = 19, scale = 2)
    private BigDecimal finalPricelist;
}
