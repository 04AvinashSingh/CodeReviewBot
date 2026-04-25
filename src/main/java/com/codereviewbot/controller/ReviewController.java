package com.codereviewbot.controller;

import com.codereviewbot.dto.ReviewDetailResponse;
import com.codereviewbot.dto.ReviewResponse;
import com.codereviewbot.entity.Review;
import com.codereviewbot.exception.ResourceNotFoundException;
import com.codereviewbot.repository.ReviewRepository;
import com.codereviewbot.security.TenantPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listReviews(
            @AuthenticationPrincipal TenantPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Review> reviews = reviewRepository.findByTenantId(
                principal.getTenantId(),
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return ResponseEntity.ok(Map.of(
                "reviews", reviews.getContent().stream().map(this::toSummary).collect(Collectors.toList()),
                "page", reviews.getNumber(),
                "totalPages", reviews.getTotalPages(),
                "totalElements", reviews.getTotalElements()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDetailResponse> getReview(
            @AuthenticationPrincipal TenantPrincipal principal,
            @PathVariable UUID id) {

        Review review = reviewRepository.findByIdAndTenantId(id, principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));

        return ResponseEntity.ok(toDetail(review));
    }

    private ReviewResponse toSummary(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .repoFullName(review.getRepo().getRepoFullName())
                .prNumber(review.getPrNumber())
                .prTitle(review.getPrTitle())
                .status(review.getStatus().name())
                .tokensUsed(review.getTokensUsed())
                .createdAt(review.getCreatedAt())
                .completedAt(review.getCompletedAt())
                .commentCount(review.getComments() != null ? review.getComments().size() : 0)
                .build();
    }

    private ReviewDetailResponse toDetail(Review review) {
        return ReviewDetailResponse.builder()
                .id(review.getId())
                .repoFullName(review.getRepo().getRepoFullName())
                .prNumber(review.getPrNumber())
                .prTitle(review.getPrTitle())
                .status(review.getStatus().name())
                .tokensUsed(review.getTokensUsed())
                .errorMessage(review.getErrorMessage())
                .createdAt(review.getCreatedAt())
                .completedAt(review.getCompletedAt())
                .comments(review.getComments().stream()
                        .map(c -> ReviewDetailResponse.ReviewCommentResponse.builder()
                                .id(c.getId())
                                .filePath(c.getFilePath())
                                .lineNumber(c.getLineNumber())
                                .severity(c.getSeverity().name())
                                .commentBody(c.getCommentBody())
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }
}
