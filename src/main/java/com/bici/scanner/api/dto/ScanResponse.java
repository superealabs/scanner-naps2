package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScanResponse {
    @JsonProperty("scanId")
    private String scanId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("createdAt")
    private String createdAt;
    
    @JsonProperty("startedAt")
    private String startedAt;
    
    @JsonProperty("completedAt")
    private String completedAt;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("pdfPath")
    private String pdfPath;
    
    @JsonProperty("pdfSize")
    private Long pdfSize;

    public ScanResponse() {
    }

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public Long getPdfSize() {
        return pdfSize;
    }

    public void setPdfSize(Long pdfSize) {
        this.pdfSize = pdfSize;
    }
}

