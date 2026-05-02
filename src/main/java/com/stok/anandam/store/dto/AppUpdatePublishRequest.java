package com.stok.anandam.store.dto;

import lombok.Data;

@Data
public class AppUpdatePublishRequest {
    private String versionName;
    private Integer versionCode;
    private String downloadUrl;
    private String releaseNotes;
    private Boolean isForceUpdate;
}

