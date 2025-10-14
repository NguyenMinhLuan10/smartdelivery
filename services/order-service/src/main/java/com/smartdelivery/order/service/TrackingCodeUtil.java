// service/TrackingCodeUtil.java
package com.smartdelivery.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class TrackingCodeUtil {
    @Value("${app.tracking.code-prefix:SD}") private String prefix;
    public String gen(){
        String day = LocalDate.now().toString().replaceAll("-", ""); // yyyyMMdd
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0,8).toUpperCase();
        return prefix + day + rand;
    }
}
