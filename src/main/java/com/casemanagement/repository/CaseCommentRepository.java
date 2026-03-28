package com.casemanagement.repository;

import com.casemanagement.entity.CaseComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseCommentRepository extends JpaRepository<CaseComment, String> {
    List<CaseComment> findByCaseEntityIdOrderByCreatedAtDesc(String caseId);
}
