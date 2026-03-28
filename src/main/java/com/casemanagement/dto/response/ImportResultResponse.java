package com.casemanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportResultResponse {
    private int totalRows;
    private int successCount;
    private int failedCount;
    private List<String> errors;
}
