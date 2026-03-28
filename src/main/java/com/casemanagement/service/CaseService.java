package com.casemanagement.service;

import com.casemanagement.dto.request.CaseRequest;
import com.casemanagement.dto.request.CommentRequest;
import com.casemanagement.dto.response.CaseResponse;
import com.casemanagement.dto.response.CommentResponse;
import com.casemanagement.dto.response.PageResponse;
import com.casemanagement.enums.CaseStatus;

public interface CaseService {

    PageResponse<CaseResponse> getAllCases(String userId, CaseStatus status, String search,
                                           int page, int size);

    CaseResponse getCaseById(String caseId);

    CaseResponse createCase(CaseRequest request);

    CaseResponse updateCase(String caseId, CaseRequest request);

    void deleteCase(String caseId);

    CommentResponse addComment(String caseId, String commentedById, CommentRequest request);
}
