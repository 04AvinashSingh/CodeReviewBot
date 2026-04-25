package com.codereviewbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "usage_tracking", uniqueConstraints = {
        @UniqueConstraint(name = "uq_usage_tenant_month", columnNames = {"tenant_id", "month_year"})
}, indexes = {
        @Index(name = "idx_usage_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_usage_month_year", columnList = "month_year")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear; // Format: "2026-04"

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "token_count", nullable = false)
    @Builder.Default
    private Long tokenCount = 0L;
}
