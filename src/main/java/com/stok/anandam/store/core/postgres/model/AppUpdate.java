package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "app_updates")
public class AppUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String versionName;

    @Column(nullable = false, unique = true)
    private Integer versionCode;

    @Column(nullable = false)
    private String downloadUrl;

    @Column(columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(nullable = false)
    private Boolean isForceUpdate = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
