package com.codereviewbot.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private UUID id;
    private String repoFullName;
    private Integer prNumber;
    private String prTitle;
    private String status;
    private Integer tokensUsed;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer commentCount;
}
