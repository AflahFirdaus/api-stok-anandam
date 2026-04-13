package com.stok.anandam.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.gson.annotations.SerializedName;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMemoTypeRequest {
    @JsonProperty("details")
    @JsonAlias("details")
    @SerializedName("details")
    private CreateMemoRequest details;
}
