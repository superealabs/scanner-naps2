package com.bici.scanner.repository.entity;

public class ScanSession {
    private String scanId;
    private ScanStatus status;
    private String scannerName;
    private String optionsJson;
    private long createdAt;
    private Long startedAt;
    private Long completedAt;
    private Long cancelledAt;
    private String errorMessage;
    private String pdfPath;
    private Long pdfSize;
    private String createdAtIso;

    public ScanSession() {
    }

    public ScanSession(String scanId, ScanStatus status, long createdAt, String createdAtIso) {
        this.scanId = scanId;
        this.status = status;
        this.createdAt = createdAt;
        this.createdAtIso = createdAtIso;
    }

    // Getters et Setters
    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public void setStatus(ScanStatus status) {
        this.status = status;
    }

    public String getScannerName() {
        return scannerName;
    }

    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public Long getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Long cancelledAt) {
        this.cancelledAt = cancelledAt;
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

    public String getCreatedAtIso() {
        return createdAtIso;
    }

    public void setCreatedAtIso(String createdAtIso) {
        this.createdAtIso = createdAtIso;
    }
}

