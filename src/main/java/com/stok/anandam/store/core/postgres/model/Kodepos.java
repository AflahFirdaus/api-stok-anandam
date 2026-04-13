package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kode_pos_diy")
public class Kodepos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "kode_pos", nullable = false, length = 10)
    private String kodePos;

    @Column(name = "desa_kelurahan", length = 100)
    private String desaKelurahan;

    @Column(name = "kecamatan", length = 100)
    private String kecamatan;

    @Column(name = "kabupaten_kota", length = 100)
    private String kabupatenKota;

    @Column(name = "latitude", columnDefinition = "NUMERIC(10,8)")
    private BigDecimal latitude;

    @Column(name = "longitude", columnDefinition = "NUMERIC(11,8)")
    private BigDecimal longitude;

    @Column(name = "provinsi", length = 100)
    private String provinsi;
}
