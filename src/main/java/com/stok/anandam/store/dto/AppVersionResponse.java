package com.stok.anandam.store.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppVersionResponse {
    private String versionName;
    private Integer versionCode;
    private String downloadUrl;
    private String releaseNotes;
    private Boolean isForceUpdate;
}
