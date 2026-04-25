package com.codereviewbot.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private String file_path;
    private Integer line_number;
    private String severity;
    private String comment;
}
