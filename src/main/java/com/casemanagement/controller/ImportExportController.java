package com.casemanagement.controller;

import com.casemanagement.dto.response.ApiResponse;
import com.casemanagement.dto.response.ImportResultResponse;
import com.casemanagement.service.ImportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/import-export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Import / Export (Admin Only)", description = "Bulk import from Excel/CSV; export to Excel/CSV; wipe DB")
@SecurityRequirement(name = "bearerAuth")
public class ImportExportController {

    private final ImportExportService importExportService;

    // ─── Import ───────────────────────────────────────────────────────────────

    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import cases from Excel (.xlsx)",
        description = "Accepts any file size. Adds rows to DB. Existing DB is NOT cleared first."
    )
    public ResponseEntity<ApiResponse<ImportResultResponse>> importExcel(
            @RequestParam("file") MultipartFile file) {
        ImportResultResponse result = importExportService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Excel import completed", result));
    }

    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Import cases from CSV",
        description = "Accepts any file size. Adds rows to DB. Existing DB is NOT cleared first."
    )
    public ResponseEntity<ApiResponse<ImportResultResponse>> importCsv(
            @RequestParam("file") MultipartFile file) {
        ImportResultResponse result = importExportService.importFromCsv(file);
        return ResponseEntity.ok(ApiResponse.success("CSV import completed", result));
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    @GetMapping("/export/excel")
    @Operation(summary = "Export all cases to Excel (.xlsx)")
    public void exportExcel(HttpServletResponse response) throws Exception {
        importExportService.exportToExcel(response);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export all cases to CSV")
    public void exportCsv(HttpServletResponse response) throws Exception {
        importExportService.exportToCsv(response);
    }

    // ─── Delete all cases ─────────────────────────────────────────────────────

    @DeleteMapping("/cases/delete-all")
    @Operation(
        summary = "Delete ALL cases from database",
        description = "WARNING: This permanently wipes every case record. Use before a fresh import."
    )
    public ResponseEntity<ApiResponse<Void>> deleteAllCases() {
        importExportService.deleteAllCases();
        return ResponseEntity.ok(ApiResponse.success("All cases deleted from database", null));
    }
}
