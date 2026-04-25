package com.codereviewbot.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageResponse {
    private String monthYear;
    private Integer reviewCount;
    private Long tokenCount;
    private Integer reviewLimit;
    private String plan;
}
