package com.convertly.dto;

import java.time.Instant;

/**
 * Data Transfer Object representing system and cloud database health status.
 */
public class HealthStatusDto {

    private String status;           // "UP", "DEGRADED", "DOWN"
    private String service;          // "Convertly Backend"
    private String databaseStatus;   // "CONNECTED", "DISCONNECTED", "NOT_CONFIGURED"
    private String databaseDetails;  // e.g. "Aiven Cloud MySQL" or error summary
    private String storageMode;      // "Ephemeral / Transient (Zero permanent local storage)"
    private Instant timestamp;

    public HealthStatusDto() {
        this.timestamp = Instant.now();
    }

    public HealthStatusDto(String status, String service, String databaseStatus, String databaseDetails, String storageMode) {
        this.status = status;
        this.service = service;
        this.databaseStatus = databaseStatus;
        this.databaseDetails = databaseDetails;
        this.storageMode = storageMode;
        this.timestamp = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }

    public String getDatabaseDetails() {
        return databaseDetails;
    }

    public void setDatabaseDetails(String databaseDetails) {
        this.databaseDetails = databaseDetails;
    }

    public String getStorageMode() {
        return storageMode;
    }

    public void setStorageMode(String storageMode) {
        this.storageMode = storageMode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
