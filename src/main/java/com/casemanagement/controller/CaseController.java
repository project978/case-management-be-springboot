package com.casemanagement.controller;

import com.casemanagement.dto.request.CaseRequest;
import com.casemanagement.dto.request.CommentRequest;
import com.casemanagement.dto.response.ApiResponse;
import com.casemanagement.dto.response.CaseResponse;
import com.casemanagement.dto.response.CommentResponse;
import com.casemanagement.dto.response.PageResponse;
import com.casemanagement.enums.CaseStatus;
import com.casemanagement.enums.UserRole;
import com.casemanagement.exception.AccessDeniedException;
import com.casemanagement.security.UserPrincipal;
import com.casemanagement.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "Case CRUD, filtering, and comments")
@SecurityRequirement(name = "bearerAuth")
public class CaseController {

    private final CaseService caseService;

    // ─── List / search with filters ───────────────────────────────────────────

    @GetMapping
    @Operation(
        summary = "List cases with optional filters",
        description = "Admin: filter by any user. User: filter by their own cases or all."
    )
    public ResponseEntity<ApiResponse<PageResponse<CaseResponse>>> getCases(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by assigned user ID (Admin only for other users)")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by HO status")
            @RequestParam(required = false) CaseStatus status,
            @Parameter(description = "Search by party name, APAC, or registration number")
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Regular USER cannot filter by another user's cases
        if (principal.getUser().getRole() == UserRole.USER
                && userId != null && !userId.equals(principal.getId())) {
            throw new AccessDeniedException("You can only view your own cases");
        }

        PageResponse<CaseResponse> cases = caseService.getAllCases(userId, status, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    // ─── Get single case ──────────────────────────────────────────────────────

    @GetMapping("/{caseId}")
    @Operation(summary = "Get case details with comments")
    public ResponseEntity<ApiResponse<CaseResponse>> getCaseById(@PathVariable String caseId) {
        return ResponseEntity.ok(ApiResponse.success(caseService.getCaseById(caseId)));
    }

    // ─── Admin: Create case ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Create a new case")
    public ResponseEntity<ApiResponse<CaseResponse>> createCase(
            @Valid @RequestBody CaseRequest request) {
        CaseResponse created = caseService.createCase(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case created", created));
    }

    // ─── Admin + User: Edit case ──────────────────────────────────────────────

    @PutMapping("/{caseId}")
    @Operation(summary = "Update a case (Admin: full update; User: can update their assigned case)")
    public ResponseEntity<ApiResponse<CaseResponse>> updateCase(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String caseId,
            @Valid @RequestBody CaseRequest request) {

        // For USER role, ensure the case is assigned to them before editing
        if (principal.getUser().getRole() == UserRole.USER) {
            CaseResponse existing = caseService.getCaseById(caseId);
            if (existing.getAssignedUser() == null
                    || !existing.getAssignedUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("You can only edit cases assigned to you");
            }
        }

        CaseResponse updated = caseService.updateCase(caseId, request);
        return ResponseEntity.ok(ApiResponse.success("Case updated", updated));
    }

    // ─── Admin: Delete case ───────────────────────────────────────────────────

    @DeleteMapping("/{caseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Delete a case")
    public ResponseEntity<ApiResponse<Void>> deleteCase(@PathVariable String caseId) {
        caseService.deleteCase(caseId);
        return ResponseEntity.ok(ApiResponse.success("Case deleted", null));
    }

    // ─── Add comment (Admin + User) ───────────────────────────────────────────

    @PostMapping("/{caseId}/comments")
    @Operation(summary = "Add a comment to a case (Admin and User)")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String caseId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse comment = caseService.addComment(caseId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", comment));
    }
}
