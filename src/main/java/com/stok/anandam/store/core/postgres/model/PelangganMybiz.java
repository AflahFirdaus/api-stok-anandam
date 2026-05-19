package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "pelanggan_mybiz", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"kode_partner"})
})
public class PelangganMybiz {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pelanggan_mybiz_seq_gen")
    @SequenceGenerator(name = "pelanggan_mybiz_seq_gen", sequenceName = "pelanggan_mybiz_seq", allocationSize = 50)
    private Long id;

    @Column(name = "kode_partner", length = 50)
    private String kodePartner;

    @Column(name = "nama_partner", length = 150)
    private String namaPartner;

    @Column(name = "mybiz_emp_id")
    private Long mybizEmpId;

    @Column(name = "kode_marketing", length = 50)
    private String kodeMarketing;

    @Column(name = "nama_marketing", length = 150)
    private String namaMarketing;

    @Column(name = "limit_piutang")
    private BigDecimal limitPiutang;

    @Column(name = "termin_piutang")
    private Integer terminPiutang;

    @Column(name = "limit_hutang")
    private BigDecimal limitHutang;

    @Column(name = "termin_hutang")
    private Integer terminHutang;

    @Column(name = "npwp", length = 50)
    private String npwp;

    @Column(name = "alamat", columnDefinition = "text")
    private String alamat;

    @Column(name = "no_telepon", length = 50)
    private String noTelepon;

    @Column(name = "last_synced")
    private LocalDateTime lastSynced;
}
