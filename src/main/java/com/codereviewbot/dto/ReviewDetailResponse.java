package com.codereviewbot.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDetailResponse {
    private UUID id;
    private String repoFullName;
    private Integer prNumber;
    private String prTitle;
    private String status;
    private Integer tokensUsed;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<ReviewCommentResponse> comments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewCommentResponse {
        private UUID id;
        private String filePath;
        private Integer lineNumber;
        private String severity;
        private String commentBody;
    }
}
