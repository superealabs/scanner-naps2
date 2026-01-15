package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthResponse {
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("naps2Available")
    private boolean naps2Available;
    
    @JsonProperty("databaseConnected")
    private boolean databaseConnected;

    public HealthResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isNaps2Available() {
        return naps2Available;
    }

    public void setNaps2Available(boolean naps2Available) {
        this.naps2Available = naps2Available;
    }

    public boolean isDatabaseConnected() {
        return databaseConnected;
    }

    public void setDatabaseConnected(boolean databaseConnected) {
        this.databaseConnected = databaseConnected;
    }
}

