package com.codereviewbot.repository;

import com.codereviewbot.entity.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepoRepository extends JpaRepository<Repo, UUID> {

    List<Repo> findByTenantId(UUID tenantId);

    Optional<Repo> findByRepoFullName(String repoFullName);

    Optional<Repo> findByRepoFullNameAndActiveTrue(String repoFullName);

    Optional<Repo> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndRepoFullName(UUID tenantId, String repoFullName);
}
