package com.casemanagement.service.impl;

import com.casemanagement.dto.request.CaseRequest;
import com.casemanagement.dto.request.CommentRequest;
import com.casemanagement.dto.response.CaseResponse;
import com.casemanagement.dto.response.CommentResponse;
import com.casemanagement.dto.response.PageResponse;
import com.casemanagement.dto.response.UserResponse;
import com.casemanagement.entity.Case;
import com.casemanagement.entity.CaseComment;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CaseStatus;
import com.casemanagement.exception.ResourceNotFoundException;
import com.casemanagement.repository.CaseCommentRepository;
import com.casemanagement.repository.CaseRepository;
import com.casemanagement.repository.CaseSpecification;
import com.casemanagement.repository.UserRepository;
import com.casemanagement.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private final CaseRepository caseRepository;
    private final CaseCommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponse<CaseResponse> getAllCases(String userId, CaseStatus status,
                                                   String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<Case> spec = CaseSpecification.withFilters(userId, status, search);
        Page<Case> cases = caseRepository.findAll(spec, pageable);
        Page<CaseResponse> responsePage = cases.map(this::toResponse);
        return PageResponse.of(responsePage);
    }

    @Override
    public CaseResponse getCaseById(String caseId) {
        Case c = findCaseById(caseId);
        return toResponseWithComments(c);
    }

    @Override
    @Transactional
    public CaseResponse createCase(CaseRequest request) {
        Case c = buildCaseFromRequest(new Case(), request);
        return toResponse(caseRepository.save(c));
    }

    @Override
    @Transactional
    public CaseResponse updateCase(String caseId, CaseRequest request) {
        Case c = findCaseById(caseId);
        buildCaseFromRequest(c, request);
        return toResponse(caseRepository.save(c));
    }

    @Override
    @Transactional
    public void deleteCase(String caseId) {
        Case c = findCaseById(caseId);
        caseRepository.delete(c);
    }

    @Override
    @Transactional
    public CommentResponse addComment(String caseId, String commentedById, CommentRequest request) {
        Case c = findCaseById(caseId);
        User user = userRepository.findById(commentedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + commentedById));

        CaseComment comment = CaseComment.builder()
                .comment(request.getComment())
                .caseEntity(c)
                .commentedBy(user)
                .build();

        CaseComment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Case findCaseById(String caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
    }

    private Case buildCaseFromRequest(Case c, CaseRequest req) {
        c.setSrNo(req.getSrNo());
        c.setApac(req.getApac());
        c.setPartyName(req.getPartyName());
        c.setCustomerContactNo(req.getCustomerContactNo());
        c.setFos(req.getFos());
        c.setFosContactNo(req.getFosContactNo());
        c.setHoStatus(req.getHoStatus());
        c.setBkt(req.getBkt());
        c.setEmi(req.getEmi());
        c.setPos(req.getPos());
        c.setCaseValue(req.getCaseValue());
        c.setLmFri(req.getLmFri());
        c.setCurFri(req.getCurFri());
        c.setOdWithoutFri(req.getOdWithoutFri());
        c.setOdWithFri(req.getOdWithFri());
        c.setMCollectId(req.getMCollectId());
        c.setAddress(req.getAddress());
        c.setAssetName(req.getAssetName());
        c.setRegistrationNumber(req.getRegistrationNumber());
        c.setEngineNo(req.getEngineNo());
        c.setChassisNo(req.getChassisNo());

        if (req.getAssignedUserId() != null) {
            User assignedUser = userRepository.findById(req.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assigned user not found: " + req.getAssignedUserId()));
            c.setAssignedUser(assignedUser);
        }
        return c;
    }

    public CaseResponse toResponse(Case c) {
        CaseResponse.CaseResponseBuilder builder = CaseResponse.builder()
                .id(c.getId())
                .srNo(c.getSrNo())
                .apac(c.getApac())
                .partyName(c.getPartyName())
                .customerContactNo(c.getCustomerContactNo())
                .fos(c.getFos())
                .fosContactNo(c.getFosContactNo())
                .hoStatus(c.getHoStatus())
                .bkt(c.getBkt())
                .emi(c.getEmi())
                .pos(c.getPos())
                .caseValue(c.getCaseValue())
                .lmFri(c.getLmFri())
                .curFri(c.getCurFri())
                .odWithoutFri(c.getOdWithoutFri())
                .odWithFri(c.getOdWithFri())
                .mCollectId(c.getMCollectId())
                .address(c.getAddress())
                .assetName(c.getAssetName())
                .registrationNumber(c.getRegistrationNumber())
                .engineNo(c.getEngineNo())
                .chassisNo(c.getChassisNo())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt());

        if (c.getAssignedUser() != null) {
            User u = c.getAssignedUser();
            builder.assignedUser(UserResponse.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .role(u.getRole())
                    .build());
        }
        return builder.build();
    }

    private CaseResponse toResponseWithComments(Case c) {
        CaseResponse response = toResponse(c);
        List<CommentResponse> comments = commentRepository
                .findByCaseEntityIdOrderByCreatedAtDesc(c.getId())
                .stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
        response.setComments(comments);
        return response;
    }

    private CommentResponse toCommentResponse(CaseComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .comment(comment.getComment())
                .commentedById(comment.getCommentedBy().getId())
                .commentedByName(comment.getCommentedBy().getName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
