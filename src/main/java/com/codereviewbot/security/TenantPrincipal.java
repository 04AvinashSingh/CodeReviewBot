package com.codereviewbot.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Holds the authenticated tenant's identity in the SecurityContext.
 */
@Getter
@AllArgsConstructor
public class TenantPrincipal {
    private final UUID tenantId;
    private final String email;
    private final String role;
}
