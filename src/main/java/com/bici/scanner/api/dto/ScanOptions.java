package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScanOptions {
    @JsonProperty("resolution")
    private Integer resolution;
    
    @JsonProperty("colorMode")
    private String colorMode;
    
    @JsonProperty("pageSize")
    private String pageSize;
    
    @JsonProperty("driver")
    private String driver;
    
    @JsonProperty("source")
    private String source;

    public ScanOptions() {
    }

    public Integer getResolution() {
        return resolution;
    }

    public void setResolution(Integer resolution) {
        this.resolution = resolution;
    }

    public String getColorMode() {
        return colorMode;
    }

    public void setColorMode(String colorMode) {
        this.colorMode = colorMode;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}

