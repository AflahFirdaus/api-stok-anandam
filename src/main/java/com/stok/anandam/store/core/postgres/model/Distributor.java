package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "distributor", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nama_distributor"})
})
public class Distributor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "distributor_seq_gen")
    @SequenceGenerator(name = "distributor_seq_gen", sequenceName = "distributor_seq", allocationSize = 50)
    private Long id;

    @Column(name = "nama_distributor", length = 200)
    private String namaDistributor;

    @Column(name = "tipe_pajak", length = 50)
    private String tipePajak;

    @Column(name = "last_synced")
    private LocalDateTime lastSynced;
}