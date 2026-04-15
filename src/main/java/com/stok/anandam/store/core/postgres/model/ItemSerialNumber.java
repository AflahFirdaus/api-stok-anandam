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
@Table(name = "item_serial_numbers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sn", "doc_id", "type"})
})
public class ItemSerialNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime tanggal;
    private String docId;
    private String userName;
    private String itemName;
    private String sn;
    private String type; // MASUK / KELUAR

    @Column(name = "last_synced")
    private LocalDateTime lastSynced;
}