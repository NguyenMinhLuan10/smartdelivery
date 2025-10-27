package com.smartdelivery.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class CreateOrderRequest {

    @Data public static class Party {
        @NotBlank private String name;
        @NotBlank private String phone;
    }

    @Data public static class Points {
        @NotBlank private String pickupAddress;
        @NotBlank private String dropoffAddress;
    }

    // ==== BẮT BUỘC (giữ nguyên chữ ký cũ) ====
    @NotBlank       private String serviceTypeCode; // STANDARD/SAME_DAY/...
    @Valid @NotNull private Party  customer;        // người gửi
    @Valid @NotNull private Points points;

    // ➕ Người nhận (FE sẽ gửi; nếu thiếu vẫn cho qua)
    @Valid private Party receiver;

    @NotNull private BigDecimal weightKg;      // nếu FE không gửi goods[] thì dùng cái này
    private BigDecimal valueAmount;            // tổng khai giá
    private Boolean    isCOD = false;
    private BigDecimal codAmount;

    // ==== MỞ RỘNG (từ Stepper FE – tuỳ chọn) ====
    @Data
    public static class GoodsItem {
        @NotBlank private String name;
        @NotNull  private Integer qty;
        @NotNull  private Double  weightKg;     // FE convert g -> kg
        @NotNull  private Integer value;        // VND
    }
    private List<GoodsItem> goods;

    private Set<String> properties;   // “Giá trị cao”, “Dễ vỡ”, …
    private Integer sizeDx;           // cm
    private Integer sizeRx;           // cm
    private Integer sizeCx;           // cm

    private Boolean addOnSms;
    private Boolean addOnInsure;
    private String  payer;            // "Người gửi"/"Người nhận"
    private Integer discount;         // VND
}
