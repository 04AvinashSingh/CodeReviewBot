package com.codereviewbot.service;

import com.codereviewbot.dto.UsageResponse;
import com.codereviewbot.entity.Tenant;
import com.codereviewbot.entity.UsageTracking;
import com.codereviewbot.entity.enums.Plan;
import com.codereviewbot.exception.RateLimitException;
import com.codereviewbot.repository.TenantRepository;
import com.codereviewbot.repository.UsageTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageTrackingService {

    private final UsageTrackingRepository usageTrackingRepository;
    private final TenantRepository tenantRepository;

    @Value("${app.free-plan-review-limit}")
    private int freeLimit;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Checks if the tenant has remaining reviews. Throws RateLimitException if over limit.
     */
    @Transactional(readOnly = true)
    public void checkLimit(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + tenantId));

        // PRO plan has unlimited reviews
        if (tenant.getPlan() == Plan.PRO) {
            return;
        }

        String currentMonth = YearMonth.now().format(MONTH_FORMAT);
        UsageTracking usage = usageTrackingRepository.findByTenantIdAndMonthYear(tenantId, currentMonth)
                .orElse(null);

        if (usage != null && usage.getReviewCount() >= freeLimit) {
            throw new RateLimitException(
                    "FREE plan limit reached: " + freeLimit + " reviews/month. " +
                    "Upgrade to PRO for unlimited reviews. Current usage: " + usage.getReviewCount()
            );
        }
    }

    /**
     * Increments the review count and token count for the current month.
     */
    @Transactional
    public void recordUsage(UUID tenantId, int tokensUsed) {
        String currentMonth = YearMonth.now().format(MONTH_FORMAT);

        Tenant tenant = tenantRepository.getReferenceById(tenantId);

        UsageTracking usage = usageTrackingRepository.findByTenantIdAndMonthYear(tenantId, currentMonth)
                .orElseGet(() -> UsageTracking.builder()
                        .tenant(tenant)
                        .monthYear(currentMonth)
                        .reviewCount(0)
                        .tokenCount(0L)
                        .build());

        usage.setReviewCount(usage.getReviewCount() + 1);
        usage.setTokenCount(usage.getTokenCount() + tokensUsed);

        usageTrackingRepository.save(usage);
        log.info("Recorded usage for tenant {}: month={}, reviews={}, tokens={}",
                tenantId, currentMonth, usage.getReviewCount(), usage.getTokenCount());
    }

    /**
     * Gets current month usage stats for a tenant.
     */
    @Transactional(readOnly = true)
    public UsageResponse getCurrentUsage(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + tenantId));

        String currentMonth = YearMonth.now().format(MONTH_FORMAT);
        UsageTracking usage = usageTrackingRepository.findByTenantIdAndMonthYear(tenantId, currentMonth)
                .orElse(UsageTracking.builder()
                        .monthYear(currentMonth)
                        .reviewCount(0)
                        .tokenCount(0L)
                        .build());

        return UsageResponse.builder()
                .monthYear(usage.getMonthYear())
                .reviewCount(usage.getReviewCount())
                .tokenCount(usage.getTokenCount())
                .reviewLimit(tenant.getPlan() == Plan.PRO ? -1 : freeLimit)
                .plan(tenant.getPlan().name())
                .build();
    }

    /**
     * Gets usage history (last 6 months) for a tenant.
     */
    @Transactional(readOnly = true)
    public List<UsageResponse> getUsageHistory(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tenant: " + tenantId));

        return usageTrackingRepository.findRecentByTenantId(tenantId).stream()
                .limit(6)
                .map(u -> UsageResponse.builder()
                        .monthYear(u.getMonthYear())
                        .reviewCount(u.getReviewCount())
                        .tokenCount(u.getTokenCount())
                        .reviewLimit(tenant.getPlan() == Plan.PRO ? -1 : freeLimit)
                        .plan(tenant.getPlan().name())
                        .build())
                .collect(Collectors.toList());
    }
}
