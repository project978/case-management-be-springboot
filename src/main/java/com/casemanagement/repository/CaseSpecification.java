package com.casemanagement.repository;

import com.casemanagement.entity.Case;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CaseStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CaseSpecification {

    private CaseSpecification() {}

    /**
     * Builds a type-safe Specification that filters cases by:
     *  - assignedUser.id  (optional)
     *  - hoStatus         (optional)
     *  - search string    (optional – matches partyName, apac, registrationNumber case-insensitively)
     */
    public static Specification<Case> withFilters(String userId, CaseStatus status, String search) {
        return (Root<Case> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ── Filter by assigned user ──────────────────────────────────────
            if (userId != null && !userId.isBlank()) {
                Join<Case, User> userJoin = root.join("assignedUser", JoinType.LEFT);
                predicates.add(cb.equal(userJoin.get("id"), userId));
            }

            // ── Filter by status ─────────────────────────────────────────────
            if (status != null) {
                predicates.add(cb.equal(root.get("hoStatus"), status));
            }

            // ── Search across partyName, apac, registrationNumber ────────────
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";

                Predicate byPartyName = cb.like(
                        cb.lower(root.get("partyName").as(String.class)), pattern);

                Predicate byApac = cb.like(
                        cb.lower(root.get("apac").as(String.class)), pattern);

                Predicate byRegNo = cb.like(
                        cb.lower(root.get("registrationNumber").as(String.class)), pattern);

                predicates.add(cb.or(byPartyName, byApac, byRegNo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
