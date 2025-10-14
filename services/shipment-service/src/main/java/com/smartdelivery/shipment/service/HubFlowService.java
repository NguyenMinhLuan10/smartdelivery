// service/HubFlowService.java
package com.smartdelivery.shipment.service;

import com.smartdelivery.shipment.client.OrderClient;
import com.smartdelivery.shipment.dto.HubArrivalRequest;
import com.smartdelivery.shipment.dto.HubOutboundRequest;
import com.smartdelivery.shipment.dto.OrderStatusChangeRequest;
import com.smartdelivery.shipment.model.Hub;
import com.smartdelivery.shipment.model.Leg;
import com.smartdelivery.shipment.repository.HubRepository;
import com.smartdelivery.shipment.repository.LegRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class HubFlowService {
    private final LegRepository legs;
    private final HubRepository hubs;
    private final OrderClient orderClient;

    private Hub requireHub(String code){ return hubs.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Hub not found: "+code)); }
    private int nextSeq(UUID orderId){ return legs.findByOrderIdOrderBySeqNoAsc(orderId).size()+1; }

    @Transactional
    public void outbound(HubOutboundRequest req, HttpServletRequest http){
        var from = requireHub(req.getFromHubCode());
        var to   = requireHub(req.getToHubCode());

        Leg leg = (req.getLegId()!=null)
                ? legs.findById(req.getLegId()).orElseThrow()
                : legs.findByOrderIdOrderBySeqNoAsc(req.getOrderId()).stream()
                .filter(l -> "LINEHAUL".equalsIgnoreCase(l.getLegType())
                        && from.getId().equals(l.getFromHubId()) && to.getId().equals(l.getToHubId()))
                .findFirst()
                .orElseGet(() -> legs.save(Leg.builder()
                        .orderId(req.getOrderId())
                        .seqNo(nextSeq(req.getOrderId()))
                        .legType("LINEHAUL").fromHubId(from.getId()).toHubId(to.getId())
                        .status("ASSIGNED").plannedStart(OffsetDateTime.now()).build()));

        leg.setStatus("IN_TRANSIT"); leg.setActualStart(OffsetDateTime.now()); legs.save(leg);

        try {
            String auth = http.getHeader("Authorization");
            orderClient.transition(leg.getOrderId(), new OrderStatusChangeRequest("DEPARTED_ORIGIN_HUB","hub_outbound"), auth);
        } catch (Exception ex){ log.warn("order transition outbound ignored: {}", ex.getMessage()); }
    }

    @Transactional
    public void arrival(HubArrivalRequest req, HttpServletRequest http){
        var dest = requireHub(req.getToHubCode());

        Leg linehaul = (req.getLegId()!=null)
                ? legs.findById(req.getLegId()).orElseThrow()
                : legs.findByOrderIdOrderBySeqNoAsc(req.getOrderId()).stream()
                .filter(l -> "LINEHAUL".equalsIgnoreCase(l.getLegType())
                        && dest.getId().equals(l.getToHubId()))
                .reduce((a,b)->b).orElseThrow(() -> new IllegalStateException("No LINEHAUL to "+req.getToHubCode()));

        linehaul.setStatus("COMPLETED"); linehaul.setActualEnd(OffsetDateTime.now()); legs.save(linehaul);

        legs.findByOrderIdOrderBySeqNoAsc(linehaul.getOrderId()).stream()
                .filter(l -> "DELIVERY".equalsIgnoreCase(l.getLegType()))
                .findFirst()
                .orElseGet(() -> legs.save(Leg.builder()
                        .orderId(linehaul.getOrderId())
                        .seqNo(nextSeq(linehaul.getOrderId()))
                        .legType("DELIVERY").fromHubId(dest.getId())
                        .status("PLANNED").plannedStart(OffsetDateTime.now()).build()));

        try {
            String auth = http.getHeader("Authorization");
            orderClient.transition(linehaul.getOrderId(), new OrderStatusChangeRequest("AT_DEST_HUB","hub_arrival"), auth);
        } catch (Exception ex){ log.warn("order transition arrival ignored: {}", ex.getMessage()); }
    }
}
