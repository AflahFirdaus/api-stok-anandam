package com.stok.anandam.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSessionDto {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("currentAction")
    private String currentAction;

    @JsonProperty("lastActive")
    private String lastActive;

    public UserSessionDto() {
    }

    public UserSessionDto(String userId, String name, String status, String currentAction, String lastActive) {
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.currentAction = currentAction;
        this.lastActive = lastActive;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(String currentAction) {
        this.currentAction = currentAction;
    }

    public String getLastActive() {
        return lastActive;
    }

    public void setLastActive(String lastActive) {
        this.lastActive = lastActive;
    }

    @Override
    public String toString() {
        return "UserSessionDto{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", currentAction='" + currentAction + '\'' +
                ", lastActive='" + lastActive + '\'' +
                '}';
    }
}