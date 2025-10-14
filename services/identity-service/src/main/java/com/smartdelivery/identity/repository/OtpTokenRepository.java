package com.smartdelivery.identity.repository;

import com.smartdelivery.identity.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    @Query("""
    select o from OtpToken o
    where o.userId = :userId and o.kind = :kind 
      and (o.usedAt is null) and (o.expiresAt > :now)
    order by o.createdAt desc
    """)
    List<OtpToken> findValid(@Param("userId") UUID userId,
                             @Param("kind") String kind,
                             @Param("now") Instant now);
}
