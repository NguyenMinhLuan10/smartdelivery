package com.smartdelivery.driver.dto;

import lombok.Data;

@Data
public class StatusReq {
    private String onlineStatus; // ONLINE/OFFLINE/BUSY
    private Boolean active;
}
