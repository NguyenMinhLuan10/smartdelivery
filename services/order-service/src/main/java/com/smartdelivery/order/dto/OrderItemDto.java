package com.smartdelivery.order.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class OrderItemDto {
    private String desc;      // name
    private Integer qty;
    private Double weight;    // kg
    private Double value;     // VND
}
