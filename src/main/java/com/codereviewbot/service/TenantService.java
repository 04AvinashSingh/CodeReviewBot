package com.codereviewbot.service;

import com.codereviewbot.dto.AuthResponse;
import com.codereviewbot.dto.LoginRequest;
import com.codereviewbot.dto.RegisterRequest;
import com.codereviewbot.entity.Tenant;
import com.codereviewbot.entity.enums.Plan;
import com.codereviewbot.entity.enums.Role;
import com.codereviewbot.repository.TenantRepository;
import com.codereviewbot.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (tenantRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        Tenant tenant = Tenant.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .githubOrgOrUser(request.getGithubOrgOrUser())
                .plan(Plan.FREE)
                .role(Role.REPO_OWNER)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Registered new tenant: {} ({})", tenant.getEmail(), tenant.getId());

        String token = jwtUtil.generateToken(tenant.getId(), tenant.getEmail(), tenant.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(tenant.getEmail())
                .plan(tenant.getPlan().name())
                .role(tenant.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), tenant.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(tenant.getId(), tenant.getEmail(), tenant.getRole().name());
        log.info("Tenant logged in: {}", tenant.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(tenant.getEmail())
                .plan(tenant.getPlan().name())
                .role(tenant.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(java.util.UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new com.codereviewbot.exception.ResourceNotFoundException("Tenant", tenantId));
    }
}
