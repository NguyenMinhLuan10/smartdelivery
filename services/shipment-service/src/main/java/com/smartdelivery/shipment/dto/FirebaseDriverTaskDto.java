package com.smartdelivery.shipment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FirebaseDriverTaskDto {
    private UUID taskId;
    private String trackingCode;
    private String type;        // PICKUP / DELIVERY
    private String status;      // ASSIGNED / ACCEPTED / ...

    // điểm tài xế phải tới NGAY BÂY GIỜ
    private String address;
    private Double lat;
    private Double lng;

    // hub gốc (dùng cho pickup sau khi lấy xong thì về hub này)
    private String hubAddress;
    private Double hubLat;
    private Double hubLng;
}
