package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScanRequest {
    @JsonProperty("scannerName")
    private String scannerName;
    
    @JsonProperty("options")
    private ScanOptions options;

    public ScanRequest() {
    }

    public String getScannerName() {
        return scannerName;
    }

    public void setScannerName(String scannerName) {
        this.scannerName = scannerName;
    }

    public ScanOptions getOptions() {
        return options;
    }

    public void setOptions(ScanOptions options) {
        this.options = options;
    }
}

