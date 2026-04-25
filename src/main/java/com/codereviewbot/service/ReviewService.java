package com.codereviewbot.service;

import com.codereviewbot.dto.CommentDTO;
import com.codereviewbot.dto.FileDiff;
import com.codereviewbot.entity.Repo;
import com.codereviewbot.entity.Review;
import com.codereviewbot.entity.ReviewComment;
import com.codereviewbot.entity.enums.ReviewStatus;
import com.codereviewbot.entity.enums.Severity;
import com.codereviewbot.repository.RepoRepository;
import com.codereviewbot.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final GitHubService gitHubService;
    private final GeminiService geminiService;
    private final UsageTrackingService usageTrackingService;
    private final RepoRepository repoRepository;
    private final ReviewRepository reviewRepository;

    @Value("${gemini.max-files-per-review}")
    private int maxFilesPerReview;

    @Value("${gemini.max-lines-per-file}")
    private int maxLinesPerFile;

    /**
     * Asynchronously processes a PR review.
     * This runs off the webhook thread so GitHub gets a 200 within 10s.
     */
    @Async("reviewExecutor")
    public void processReviewAsync(String repoFullName, int prNumber, String prTitle) {
        log.info("Starting async review for PR #{} in {}", prNumber, repoFullName);

        // Find the repo registration
        Repo repo = repoRepository.findByRepoFullNameAndActiveTrue(repoFullName).orElse(null);
        if (repo == null) {
            log.warn("Repo not registered or inactive: {}", repoFullName);
            return;
        }

        UUID tenantId = repo.getTenant().getId();

        // Check rate limit
        try {
            usageTrackingService.checkLimit(tenantId);
        } catch (Exception e) {
            log.warn("Rate limit hit for tenant {} on repo {}: {}", tenantId, repoFullName, e.getMessage());
            return;
        }

        // Create review record
        Review review = Review.builder()
                .repo(repo)
                .prNumber(prNumber)
                .prTitle(prTitle)
                .status(ReviewStatus.IN_PROGRESS)
                .build();
        review = reviewRepository.save(review);

        try {
            // 1. Fetch PR diff from GitHub
            GitHub client = gitHubService.getInstallationClient(repo.getGithubInstallationId());
            List<FileDiff> diffs = gitHubService.getPullRequestDiff(client, repoFullName, prNumber);

            // 2. Limit files to prevent excessive API calls
            if (diffs.size() > maxFilesPerReview) {
                log.info("PR has {} files, limiting to {}", diffs.size(), maxFilesPerReview);
                diffs = diffs.subList(0, maxFilesPerReview);
            }

            // 3. Send each file to Gemini for review
            List<CommentDTO> allComments = new ArrayList<>();
            int totalTokens = 0;

            for (FileDiff diff : diffs) {
                // Skip files with too many lines
                if (diff.getPatch() != null && diff.getPatch().lines().count() > maxLinesPerFile) {
                    log.info("Skipping large file: {} ({} lines)", diff.getFilename(), diff.getPatch().lines().count());
                    continue;
                }

                GeminiService.GeminiResult result = geminiService.reviewDiff(diff.getFilename(), diff.getPatch());
                allComments.addAll(result.comments());
                totalTokens += result.tokensUsed();
            }

            // 4. Save comments to database
            for (CommentDTO commentDTO : allComments) {
                ReviewComment comment = ReviewComment.builder()
                        .filePath(commentDTO.getFile_path())
                        .lineNumber(commentDTO.getLine_number())
                        .severity(parseSeverity(commentDTO.getSeverity()))
                        .commentBody(commentDTO.getComment())
                        .build();
                review.addComment(comment);
            }

            // 5. Post comments on GitHub PR
            if (!allComments.isEmpty()) {
                gitHubService.postReviewComments(client, repoFullName, prNumber, allComments);
            }

            // 6. Update review status
            review.setStatus(ReviewStatus.COMPLETED);
            review.setTokensUsed(totalTokens);
            review.setCompletedAt(LocalDateTime.now());
            reviewRepository.save(review);

            // 7. Record usage
            usageTrackingService.recordUsage(tenantId, totalTokens);

            log.info("Review completed for PR #{} in {}: {} comments, {} tokens",
                    prNumber, repoFullName, allComments.size(), totalTokens);

        } catch (Exception e) {
            log.error("Review failed for PR #{} in {}: {}", prNumber, repoFullName, e.getMessage(), e);
            review.setStatus(ReviewStatus.FAILED);
            review.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(1000, e.getMessage().length())) : "Unknown error");
            review.setCompletedAt(LocalDateTime.now());
            reviewRepository.save(review);
        }
    }

    private Severity parseSeverity(String severity) {
        if (severity == null) return Severity.INFO;
        try {
            return Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Severity.INFO;
        }
    }
}
