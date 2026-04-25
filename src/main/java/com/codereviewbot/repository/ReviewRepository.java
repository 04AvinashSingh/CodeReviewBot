package com.codereviewbot.repository;

import com.codereviewbot.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("SELECT r FROM Review r WHERE r.repo.tenant.id = :tenantId")
    Page<Review> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.comments WHERE r.id = :id AND r.repo.tenant.id = :tenantId")
    Optional<Review> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Review r WHERE r.repo.id = :repoId")
    Page<Review> findByRepoId(@Param("repoId") UUID repoId, Pageable pageable);
}
