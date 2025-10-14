// dto/OrderItemDto.java
package com.smartdelivery.order.dto;

import lombok.Data;

@Data
public class OrderItemDto {
    private String desc;
    private Integer qty;
    private Double weight;
    private Double value;
}
