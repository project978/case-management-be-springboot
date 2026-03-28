package com.casemanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commented_by", nullable = false)
    private User commentedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
