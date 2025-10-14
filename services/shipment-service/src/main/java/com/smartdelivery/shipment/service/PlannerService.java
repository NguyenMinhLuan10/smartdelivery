// service/PlannerService.java
package com.smartdelivery.shipment.service;

import com.smartdelivery.shipment.dto.OrderTrackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class PlannerService {
    private static final BigDecimal DEFAULT_INTERCITY_DISTANCE_KM = new BigDecimal("15");

    public enum RouteKind { INTRACITY_DIRECT, INTERCITY_VIA_HUB }
    public record PlanResult(RouteKind kind){}

    public PlanResult classify(OrderTrackResponse o){
        if (o.getServiceTypeCode()!=null) {
            String svc = o.getServiceTypeCode().toUpperCase(Locale.ROOT);
            if (svc.contains("INTERCITY") || svc.contains("LIEN_TINH")) return new PlanResult(RouteKind.INTERCITY_VIA_HUB);
        }
        if (o.getPickupProvince()!=null && o.getDropoffProvince()!=null &&
                !o.getPickupProvince().equalsIgnoreCase(o.getDropoffProvince()))
            return new PlanResult(RouteKind.INTERCITY_VIA_HUB);

        if (o.getDistanceKm()!=null && o.getDistanceKm().compareTo(DEFAULT_INTERCITY_DISTANCE_KM)>0)
            return new PlanResult(RouteKind.INTERCITY_VIA_HUB);

        return new PlanResult(RouteKind.INTRACITY_DIRECT);
    }

    public String suggestLegTypeForAssignment(PlanResult plan){
        return plan.kind()==RouteKind.INTRACITY_DIRECT ? "DELIVERY" : "PICKUP";
    }
}
