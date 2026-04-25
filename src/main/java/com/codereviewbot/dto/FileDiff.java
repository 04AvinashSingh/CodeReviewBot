package com.codereviewbot.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDiff {
    private String filename;
    private String patch;
    private String status; // added, modified, removed, renamed
}
