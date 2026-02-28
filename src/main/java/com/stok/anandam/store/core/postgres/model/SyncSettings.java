package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "sync_settings")
public class SyncSettings {

    @Id
    @Column(name = "sync_key", length = 100)
    private String syncKey;

    @Column(name = "sync_value", length = 500)
    private String syncValue;
}
