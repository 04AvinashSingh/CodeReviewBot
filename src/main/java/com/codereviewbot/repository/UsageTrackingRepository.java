package com.codereviewbot.repository;

import com.codereviewbot.entity.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, UUID> {

    Optional<UsageTracking> findByTenantIdAndMonthYear(UUID tenantId, String monthYear);

    @Query("SELECT u FROM UsageTracking u WHERE u.tenant.id = :tenantId ORDER BY u.monthYear DESC")
    List<UsageTracking> findRecentByTenantId(@Param("tenantId") UUID tenantId);
}
