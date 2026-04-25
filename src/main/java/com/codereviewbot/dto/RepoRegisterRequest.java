package com.codereviewbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepoRegisterRequest {

    @NotBlank(message = "Repository full name is required (e.g., owner/repo)")
    private String repoFullName;

    @NotNull(message = "GitHub installation ID is required")
    private Long githubInstallationId;
}
