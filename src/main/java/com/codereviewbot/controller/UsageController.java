package com.codereviewbot.controller;

import com.codereviewbot.dto.UsageResponse;
import com.codereviewbot.security.TenantPrincipal;
import com.codereviewbot.service.UsageTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageTrackingService usageTrackingService;

    @GetMapping
    public ResponseEntity<UsageResponse> getCurrentUsage(@AuthenticationPrincipal TenantPrincipal principal) {
        UsageResponse usage = usageTrackingService.getCurrentUsage(principal.getTenantId());
        return ResponseEntity.ok(usage);
    }

    @GetMapping("/history")
    public ResponseEntity<List<UsageResponse>> getUsageHistory(@AuthenticationPrincipal TenantPrincipal principal) {
        List<UsageResponse> history = usageTrackingService.getUsageHistory(principal.getTenantId());
        return ResponseEntity.ok(history);
    }
}
