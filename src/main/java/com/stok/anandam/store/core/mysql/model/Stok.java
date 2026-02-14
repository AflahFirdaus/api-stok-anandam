package com.stok.anandam.store.core.mysql.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stok")
public class Stok {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(name = "kategori_nama", length = 50)
    private String kategoriNama;

    @Column(name = "kategori_itemcode", length = 50)
    private String kategoriItemcode;

    @Column(name = "final_stok")
    private Integer finalStok;

    @Column(name = "harga_hpp")
    private Integer hargaHpp;

    @Column(name = "grand_total")
    private Integer grandTotal;

    @Column(name = "warehouse", length = 100)
    private String warehouse;
}
