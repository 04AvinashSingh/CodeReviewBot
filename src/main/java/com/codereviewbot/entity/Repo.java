package com.codereviewbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repos", uniqueConstraints = {
        @UniqueConstraint(name = "uq_repos_tenant_repo", columnNames = {"tenant_id", "repo_full_name"})
}, indexes = {
        @Index(name = "idx_repos_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_repos_installation_id", columnList = "github_installation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "repo_full_name", nullable = false, length = 255)
    private String repoFullName;

    @Column(name = "github_installation_id", nullable = false)
    private Long githubInstallationId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
