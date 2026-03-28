package com.casemanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    private String id;
    private String comment;
    private String commentedById;
    private String commentedByName;
    private LocalDateTime createdAt;
}
