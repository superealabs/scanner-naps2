package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeviceListResponse {
    @JsonProperty("devices")
    private List<DeviceInfo> devices;
    
    @JsonProperty("count")
    private int count;

    public DeviceListResponse() {
    }

    public DeviceListResponse(List<DeviceInfo> devices) {
        this.devices = devices;
        this.count = devices != null ? devices.size() : 0;
    }

    public List<DeviceInfo> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceInfo> devices) {
        this.devices = devices;
        this.count = devices != null ? devices.size() : 0;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

