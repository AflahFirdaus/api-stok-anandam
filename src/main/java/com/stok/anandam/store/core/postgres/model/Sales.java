package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "sales") // Nama tabel di database baru
public class Sales {

    @Id
    // Optimasi Batch Insert: Gunakan SEQUENCE agar Hibernate bisa menumpuk insert
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sales_seq_gen")
    @SequenceGenerator(name = "sales_seq_gen", sequenceName = "sales_seq", allocationSize = 50)
    private Long id;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(name = "doc_no")
    private String docNo;

    @Column(name = "code", length = 50) // Partner Code
    private String code;

    @Column(name = "par_name")
    private String parName;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "grand_total")
    private BigDecimal grandTotal;

    @Column(name = "emp_code", length = 50)
    private String empCode;
}