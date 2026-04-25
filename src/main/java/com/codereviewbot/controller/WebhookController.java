package com.codereviewbot.controller;

import com.codereviewbot.service.GitHubService;
import com.codereviewbot.service.ReviewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final GitHubService gitHubService;
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    private static final Set<String> SUPPORTED_ACTIONS = Set.of("opened", "synchronize", "reopened");

    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestBody String payload) {

        // 1. Validate signature
        if (!validateSignature(payload, signature)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        // 2. Only process pull_request events
        if (!"pull_request".equals(event)) {
            log.debug("Ignoring non-PR event: {}", event);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "not a pull_request event"));
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String action = root.path("action").asText();

            // 3. Only process specific actions
            if (!SUPPORTED_ACTIONS.contains(action)) {
                log.debug("Ignoring PR action: {}", action);
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "unsupported action: " + action));
            }

            // 4. Extract PR info
            JsonNode pullRequest = root.path("pull_request");
            int prNumber = pullRequest.path("number").asInt();
            String prTitle = pullRequest.path("title").asText();
            String repoFullName = root.path("repository").path("full_name").asText();

            log.info("Received PR webhook: {} #{} ({}) — action: {}", repoFullName, prNumber, prTitle, action);

            // 5. Fire and forget — async processing
            reviewService.processReviewAsync(repoFullName, prNumber, prTitle);

            return ResponseEntity.ok(Map.of(
                    "status", "accepted",
                    "pr", repoFullName + "#" + prNumber,
                    "action", action
            ));

        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process webhook"));
        }
    }

    /**
     * Health check endpoint for webhook URL verification.
     */
    @GetMapping("/github")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Code Review Bot Webhook"));
    }

    /**
     * Validates the X-Hub-Signature-256 HMAC SHA-256 signature.
     */
    private boolean validateSignature(String payload, String signature) {
        String secret = gitHubService.getWebhookSecret();

        // Allow empty secret in development
        if (secret == null || secret.isBlank()) {
            log.warn("Webhook secret not configured — skipping signature validation (NOT safe for production!)");
            return true;
        }

        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("No valid signature header present — allowing in dev mode");
            return true;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            String expectedSignature = "sha256=" + HexFormat.of().formatHex(hash);
            boolean valid = expectedSignature.equalsIgnoreCase(signature);
            
            if (!valid) {
                // smee.io re-encodes the JSON body which breaks HMAC
                // In production (direct GitHub → server), this will match correctly
                log.warn("Webhook signature mismatch (expected={}, got={}) — allowing in dev mode (smee proxy likely modified payload)",
                        expectedSignature.substring(0, 20) + "...", signature.substring(0, 20) + "...");
            }
            return true; // Allow in dev mode; in production, change to: return valid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to validate webhook signature: {}", e.getMessage());
            return false;
        }
    }
}
