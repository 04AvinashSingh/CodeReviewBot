package com.codereviewbot.entity;

import com.codereviewbot.entity.enums.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "review_comments", indexes = {
        @Index(name = "idx_review_comments_review_id", columnList = "review_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Severity severity = Severity.INFO;

    @Column(name = "comment_body", nullable = false, length = 2000)
    private String commentBody;
}
