package com.codereviewbot.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepoResponse {
    private UUID id;
    private String repoFullName;
    private Long githubInstallationId;
    private Boolean active;
    private LocalDateTime createdAt;
}
