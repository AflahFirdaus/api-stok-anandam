package com.stok.anandam.store.core.mysql.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "sales")
public class Sales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(name = "doc_no")
    private String docNo;

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "par_name")
    private String parName;

    @Column(name = "item_name")
    private String itemName;

    private Integer qty;
    private Integer price;

    @Column(name = "grand_total")
    private Integer grandTotal;

    @Column(name = "emp_code", length = 50)
    private String empCode;
}
