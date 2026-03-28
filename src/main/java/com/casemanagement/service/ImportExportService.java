package com.casemanagement.service;

import com.casemanagement.dto.response.ImportResultResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImportExportService {

    ImportResultResponse importFromExcel(MultipartFile file);

    ImportResultResponse importFromCsv(MultipartFile file);

    void exportToExcel(HttpServletResponse response) throws Exception;

    void exportToCsv(HttpServletResponse response) throws Exception;

    void deleteAllCases();
}
