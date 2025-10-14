// dto/OrderStatusChangeRequest.java
package com.smartdelivery.shipment.dto;
import lombok.AllArgsConstructor; import lombok.Data;
@Data @AllArgsConstructor
public class OrderStatusChangeRequest { private String toStatus; private String reason; }
