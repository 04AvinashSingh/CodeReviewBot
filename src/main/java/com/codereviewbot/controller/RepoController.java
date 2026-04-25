package com.codereviewbot.controller;

import com.codereviewbot.dto.RepoRegisterRequest;
import com.codereviewbot.dto.RepoResponse;
import com.codereviewbot.entity.Repo;
import com.codereviewbot.entity.Tenant;
import com.codereviewbot.exception.ResourceNotFoundException;
import com.codereviewbot.repository.RepoRepository;
import com.codereviewbot.security.TenantPrincipal;
import com.codereviewbot.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final RepoRepository repoRepository;
    private final TenantService tenantService;

    @GetMapping
    public ResponseEntity<List<RepoResponse>> listRepos(@AuthenticationPrincipal TenantPrincipal principal) {
        List<RepoResponse> repos = repoRepository.findByTenantId(principal.getTenantId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(repos);
    }

    @PostMapping("/register")
    public ResponseEntity<RepoResponse> registerRepo(
            @AuthenticationPrincipal TenantPrincipal principal,
            @Valid @RequestBody RepoRegisterRequest request) {

        if (repoRepository.existsByTenantIdAndRepoFullName(principal.getTenantId(), request.getRepoFullName())) {
            throw new IllegalArgumentException("Repo already registered: " + request.getRepoFullName());
        }

        Tenant tenant = tenantService.getTenantById(principal.getTenantId());

        Repo repo = Repo.builder()
                .tenant(tenant)
                .repoFullName(request.getRepoFullName())
                .githubInstallationId(request.getGithubInstallationId())
                .active(true)
                .build();

        repo = repoRepository.save(repo);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repo));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RepoResponse> toggleRepo(
            @AuthenticationPrincipal TenantPrincipal principal,
            @PathVariable UUID id) {

        Repo repo = repoRepository.findByIdAndTenantId(id, principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Repo", id));

        repo.setActive(!repo.getActive());
        repo = repoRepository.save(repo);

        return ResponseEntity.ok(toResponse(repo));
    }

    private RepoResponse toResponse(Repo repo) {
        return RepoResponse.builder()
                .id(repo.getId())
                .repoFullName(repo.getRepoFullName())
                .githubInstallationId(repo.getGithubInstallationId())
                .active(repo.getActive())
                .createdAt(repo.getCreatedAt())
                .build();
    }
}
