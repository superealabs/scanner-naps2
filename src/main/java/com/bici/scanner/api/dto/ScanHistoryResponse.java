package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ScanHistoryResponse {
    @JsonProperty("scans")
    private List<ScanResponse> scans;
    
    @JsonProperty("total")
    private int total;
    
    @JsonProperty("limit")
    private int limit;
    
    @JsonProperty("offset")
    private int offset;

    public ScanHistoryResponse() {
    }

    public List<ScanResponse> getScans() {
        return scans;
    }

    public void setScans(List<ScanResponse> scans) {
        this.scans = scans;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}

