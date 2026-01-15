package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ErrorResponse {
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("scanId")
    private String scanId;

    public ErrorResponse() {
    }

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }
}

