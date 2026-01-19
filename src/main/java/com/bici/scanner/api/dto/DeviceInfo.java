package com.bici.scanner.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceInfo {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("driver")
    private String driver;
    
    @JsonProperty("id")
    private String id;

    public DeviceInfo() {
    }

    public DeviceInfo(String name, String driver, String id) {
        this.name = name;
        this.driver = driver;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

