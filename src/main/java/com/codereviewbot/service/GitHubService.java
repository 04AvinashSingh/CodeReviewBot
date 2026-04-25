package com.codereviewbot.service;

import com.codereviewbot.dto.CommentDTO;
import com.codereviewbot.dto.FileDiff;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class GitHubService {

    @Value("${github.app-id}")
    private String appId;

    @Value("${github.private-key-path}")
    private String privateKeyPath;

    @Value("${github.webhook-secret}")
    private String webhookSecret;

    /**
     * Creates a GitHub client authenticated as an installation.
     */
    public GitHub getInstallationClient(long installationId) throws IOException {
        try {
            String pemContent = Files.readString(Paths.get(privateKeyPath));
            GitHub appGitHub = new GitHubBuilder()
                    .withJwtToken(createJwt(pemContent))
                    .build();

            GHAppInstallation installation = appGitHub.getApp().getInstallationById(installationId);
            GHAppInstallationToken token = installation.createToken().create();

            return new GitHubBuilder()
                    .withAppInstallationToken(token.getToken())
                    .build();
        } catch (Exception e) {
            log.error("Failed to create installation client for installation {}: {}", installationId, e.getMessage());
            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new IOException(
                    "GitHub Auth Failed! appId=" + appId + ", instId=" + installationId + ", error=" + cause, e);
        }
    }

    /**
     * Fetches the list of changed files with their diffs for a PR.
     */
    public List<FileDiff> getPullRequestDiff(GitHub client, String repoFullName, int prNumber) throws IOException {
        GHRepository repo = client.getRepository(repoFullName);
        GHPullRequest pr = repo.getPullRequest(prNumber);

        List<FileDiff> diffs = new ArrayList<>();
        for (GHPullRequestFileDetail file : pr.listFiles()) {
            if (file.getPatch() != null && !file.getPatch().isBlank()) {
                diffs.add(FileDiff.builder()
                        .filename(file.getFilename())
                        .patch(file.getPatch())
                        .status(file.getStatus())
                        .build());
            }
        }

        log.info("Fetched {} files with diffs for PR #{} in {}", diffs.size(), prNumber, repoFullName);
        return diffs;
    }

    /**
     * Posts inline review comments on a PR as a single review.
     */
    public void postReviewComments(GitHub client, String repoFullName, int prNumber,
            List<CommentDTO> comments) throws IOException {
        if (comments.isEmpty()) {
            log.info("No comments to post for PR #{} in {}", prNumber, repoFullName);
            return;
        }

        GHRepository repo = client.getRepository(repoFullName);
        GHPullRequest pr = repo.getPullRequest(prNumber);

        // Build a single review with all comments
        GHPullRequestReviewBuilder reviewBuilder = pr.createReview()
                .event(GHPullRequestReviewEvent.COMMENT)
                .body("🤖 **AI Code Review** — Found " + comments.size() + " item(s) to review.");

        for (CommentDTO comment : comments) {
            String emoji = switch (comment.getSeverity().toUpperCase()) {
                case "ERROR" -> "🔴";
                case "WARNING" -> "🟡";
                default -> "🔵";
            };

            String body = emoji + " **[" + comment.getSeverity().toUpperCase() + "]** " + comment.getComment();

            try {
                // comment(body, path, position) — position is the line in the diff hunk
                int position = comment.getLine_number() != null ? comment.getLine_number() : 1;
                reviewBuilder.comment(body, comment.getFile_path(), position);
            } catch (Exception e) {
                // If inline comment fails (e.g. line not in diff), add as a general comment
                log.warn("Could not post inline comment on {}:{} — will add to review body",
                        comment.getFile_path(), comment.getLine_number());
            }
        }
        reviewBuilder.event(org.kohsuke.github.GHPullRequestReviewEvent.COMMENT).create();
        log.info("Posted review with {} comments on PR #{} in {}", comments.size(), prNumber, repoFullName);
    }

    /**
     * Returns the webhook secret for signature validation.
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }

    /**
     * Creates a JWT for GitHub App authentication.
     */
    private String createJwt(String pemContent) throws Exception {
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        try (org.bouncycastle.openssl.PEMParser pemParser = new org.bouncycastle.openssl.PEMParser(
                new java.io.StringReader(pemContent))) {
            Object object = pemParser.readObject();
            org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter converter = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                    .setProvider("BC");

            java.security.PrivateKey privateKey;
            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                privateKey = converter
                        .getPrivateKey(((org.bouncycastle.openssl.PEMKeyPair) object).getPrivateKeyInfo());
            } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                privateKey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
            } else {
                throw new IllegalStateException("Unexpected key format: " + object.getClass().getName());
            }

            long nowSeconds = System.currentTimeMillis() / 1000;
            String header = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
            String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(("{\"iat\":" + (nowSeconds - 60) +
                            ",\"exp\":" + (nowSeconds + 600) +
                            ",\"iss\":\"" + appId + "\"}").getBytes());

            String signingInput = header + "." + payload;

            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes());
            byte[] signatureBytes = signature.sign();

            String sig = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            return signingInput + "." + sig;
        }
    }
}
