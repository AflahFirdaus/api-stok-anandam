package com.stok.anandam.store.core.postgres.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "app_updates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"versionCode", "platform"})
})
public class AppUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String versionName;

    @Column(nullable = false)
    private Integer versionCode;

    @Column(nullable = false)
    private String platform = "ANDROID";

    @Column(nullable = false)
    private String downloadUrl;

    @Column(columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(nullable = false)
    private Boolean isForceUpdate = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
