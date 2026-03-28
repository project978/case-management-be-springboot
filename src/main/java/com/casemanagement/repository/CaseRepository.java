package com.casemanagement.repository;

import com.casemanagement.entity.Case;
import com.casemanagement.enums.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseRepository extends JpaRepository<Case, String>, JpaSpecificationExecutor<Case> {

    boolean existsByApac(String apac);
}
