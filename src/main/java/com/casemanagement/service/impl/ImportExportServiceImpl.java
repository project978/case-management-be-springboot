package com.casemanagement.service.impl;

import com.casemanagement.dto.response.ImportResultResponse;
import com.casemanagement.entity.Case;
import com.casemanagement.enums.CaseStatus;
import com.casemanagement.repository.CaseRepository;
import com.casemanagement.service.ImportExportService;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportExportServiceImpl implements ImportExportService {

    private final CaseRepository caseRepository;

    // ─── Verified column indices (cross-checked against actual DB output) ──────
    // Excel header row is shifted — actual data layout confirmed by comparing
    // imported DB records with source Excel rows field-by-field.
    //
    // col[0]  = sr_no
    // col[1]  = apac
    // col[2]  = customer_contact_id  (numeric ID like 64002543 — stored in customerContactNo)
    // col[3]  = party_name
    // col[4]  = customer_contact_no  (phone number)
    // col[5]  = fos
    // col[6]  = fos_contact_no
    // col[7]  = ho_status            (FLOW / CURED / RB / STAB / ECS)
    // col[8]  = bkt                  (PTP / Cured / Stab / Roll back)
    // col[9]  = bkt_label            (e.g. "BKT 1" — ignored, not stored)
    // col[10] = pos
    // col[11] = case_value
    // col[12] = lm_fri
    // col[13] = cur_fri
    // col[14] = od_without_fri
    // col[15] = od_with_fri
    // col[16] = emi                  (String)
    // col[17] = m_collect_id
    // col[18] = address
    // col[19] = asset_name
    // col[20] = registration_number
    // col[21] = engine_no
    // col[22] = chassis_no

    private static final int COL_SR_NO              = 0;
    private static final int COL_APAC               = 1;
    private static final int COL_CUSTOMER_CONTACT_ID = 2;  // numeric customer ID
    private static final int COL_PARTY_NAME         = 3;
    private static final int COL_CUSTOMER_CONTACT   = 4;   // phone number
    private static final int COL_FOS                = 5;
    private static final int COL_FOS_CONTACT        = 6;
    private static final int COL_HO_STATUS          = 7;
    private static final int COL_BKT                = 8;
    // col[9] = bkt_label — skipped
    private static final int COL_POS                = 10;
    private static final int COL_CASE_VALUE         = 11;
    private static final int COL_LM_FRI             = 12;
    private static final int COL_CUR_FRI            = 13;
    private static final int COL_OD_WITHOUT_FRI     = 14;
    private static final int COL_OD_WITH_FRI        = 15;
    private static final int COL_EMI                = 16;
    private static final int COL_M_COLLECT_ID       = 17;
    private static final int COL_ADDRESS            = 18;
    private static final int COL_ASSET_NAME         = 19;
    private static final int COL_REG_NO             = 20;
    private static final int COL_ENGINE_NO          = 21;
    private static final int COL_CHASSIS_NO         = 22;

    // ─── Import Excel ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImportResultResponse importFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext()) rows.next();

            while (rows.hasNext()) {
                Row row = rows.next();
                totalRows++;
                try {
                    Case c = mapRowToCase(row);
                    if (c != null) {
                        caseRepository.save(c);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    log.error("Error importing row {}: {}", row.getRowNum() + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read Excel file: " + e.getMessage());
        }

        return ImportResultResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(totalRows - successCount)
                .errors(errors)
                .build();
    }

    // ─── Import CSV ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImportResultResponse importFromCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            // Skip header
            reader.readNext();

            String[] line;
            int rowNum = 1;
            while ((line = reader.readNext()) != null) {
                totalRows++;
                rowNum++;
                try {
                    Case c = mapCsvRowToCase(line);
                    if (c != null) {
                        caseRepository.save(c);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read CSV file: " + e.getMessage());
        }

        return ImportResultResponse.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(totalRows - successCount)
                .errors(errors)
                .build();
    }

    // ─── Export Excel ─────────────────────────────────────────────────────────

    @Override
    public void exportToExcel(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=cases_export.xlsx");

        List<Case> cases = caseRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CASES");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {
                "SR_NO", "APAC", "PARTY_NAME", "CUSTOMER_CONTACT_NO", "FOS",
                "FOS_CONTACT_NO", "HO_STATUS", "BKT", "EMI", "POS",
                "CASE_VALUE", "LM_FRI", "CUR_FRI", "OD_WITHOUT_FRI", "OD_WITH_FRI",
                "M_COLLECT_ID", "ADDRESS", "ASSET_NAME", "REGISTRATION_NUMBER",
                "ENGINE_NO", "CHASSIS_NO", "ASSIGNED_USER"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Data rows
            int rowIdx = 1;
            for (Case c : cases) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getSrNo() != null ? c.getSrNo() : rowIdx - 1);
                row.createCell(1).setCellValue(nullSafe(c.getApac()));
                row.createCell(2).setCellValue(nullSafe(c.getPartyName()));
                row.createCell(3).setCellValue(nullSafe(c.getCustomerContactNo()));
                row.createCell(4).setCellValue(nullSafe(c.getFos()));
                row.createCell(5).setCellValue(nullSafe(c.getFosContactNo()));
                row.createCell(6).setCellValue(c.getHoStatus() != null ? c.getHoStatus().name() : "");
                row.createCell(7).setCellValue(nullSafe(c.getBkt()));
                row.createCell(8).setCellValue(c.getEmi() != null ? c.getEmi() : "");
                row.createCell(9).setCellValue(c.getPos() != null ? c.getPos().doubleValue() : 0);
                row.createCell(10).setCellValue(c.getCaseValue() != null ? c.getCaseValue().doubleValue() : 0);
                row.createCell(11).setCellValue(c.getLmFri() != null ? c.getLmFri().doubleValue() : 0);
                row.createCell(12).setCellValue(c.getCurFri() != null ? c.getCurFri().doubleValue() : 0);
                row.createCell(13).setCellValue(c.getOdWithoutFri() != null ? c.getOdWithoutFri().doubleValue() : 0);
                row.createCell(14).setCellValue(c.getOdWithFri() != null ? c.getOdWithFri().doubleValue() : 0);
                row.createCell(15).setCellValue(nullSafe(c.getMCollectId()));
                row.createCell(16).setCellValue(nullSafe(c.getAddress()));
                row.createCell(17).setCellValue(nullSafe(c.getAssetName()));
                row.createCell(18).setCellValue(nullSafe(c.getRegistrationNumber()));
                row.createCell(19).setCellValue(nullSafe(c.getEngineNo()));
                row.createCell(20).setCellValue(nullSafe(c.getChassisNo()));
                row.createCell(21).setCellValue(c.getAssignedUser() != null ? c.getAssignedUser().getName() : "");
            }

            workbook.write(response.getOutputStream());
        }
    }

    // ─── Export CSV ───────────────────────────────────────────────────────────

    @Override
    public void exportToCsv(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=cases_export.csv");

        List<Case> cases = caseRepository.findAll();

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(response.getOutputStream()))) {
            // Header
            writer.writeNext(new String[]{
                "SR_NO", "APAC", "PARTY_NAME", "CUSTOMER_CONTACT_NO", "FOS",
                "FOS_CONTACT_NO", "HO_STATUS", "BKT", "EMI", "POS",
                "CASE_VALUE", "LM_FRI", "CUR_FRI", "OD_WITHOUT_FRI", "OD_WITH_FRI",
                "M_COLLECT_ID", "ADDRESS", "ASSET_NAME", "REGISTRATION_NUMBER",
                "ENGINE_NO", "CHASSIS_NO", "ASSIGNED_USER"
            });

            for (Case c : cases) {
                writer.writeNext(new String[]{
                    String.valueOf(c.getSrNo()),
                    nullSafe(c.getApac()),
                    nullSafe(c.getPartyName()),
                    nullSafe(c.getCustomerContactNo()),
                    nullSafe(c.getFos()),
                    nullSafe(c.getFosContactNo()),
                    c.getHoStatus() != null ? c.getHoStatus().name() : "",
                    nullSafe(c.getBkt()),
                    c.getEmi() != null ? c.getEmi() : "",
                    c.getPos() != null ? c.getPos().toPlainString() : "",
                    c.getCaseValue() != null ? c.getCaseValue().toPlainString() : "",
                    c.getLmFri() != null ? c.getLmFri().toPlainString() : "",
                    c.getCurFri() != null ? c.getCurFri().toPlainString() : "",
                    c.getOdWithoutFri() != null ? c.getOdWithoutFri().toPlainString() : "",
                    c.getOdWithFri() != null ? c.getOdWithFri().toPlainString() : "",
                    nullSafe(c.getMCollectId()),
                    nullSafe(c.getAddress()),
                    nullSafe(c.getAssetName()),
                    nullSafe(c.getRegistrationNumber()),
                    nullSafe(c.getEngineNo()),
                    nullSafe(c.getChassisNo()),
                    c.getAssignedUser() != null ? c.getAssignedUser().getName() : ""
                });
            }
        }
    }

    // ─── Delete all cases ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteAllCases() {
        caseRepository.deleteAll();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Case mapRowToCase(Row row) {
        // Skip completely empty rows
        if (row == null || isRowEmpty(row)) return null;

        Case c = new Case();
        c.setSrNo(getCellInt(row, COL_SR_NO));
        c.setApac(getCellString(row, COL_APAC));
        c.setPartyName(getCellString(row, COL_PARTY_NAME));
        // col[2] is a numeric customer ID — store as customerContactNo only if col[4] (phone) is absent
        String phone = getCellString(row, COL_CUSTOMER_CONTACT);
        c.setCustomerContactNo(phone != null ? phone : getCellString(row, COL_CUSTOMER_CONTACT_ID));
        c.setFos(getCellString(row, COL_FOS));
        c.setFosContactNo(getCellString(row, COL_FOS_CONTACT));
        c.setHoStatus(parseStatus(getCellString(row, COL_HO_STATUS)));
        c.setBkt(getCellString(row, COL_BKT));
        // col[9] = bkt_label (e.g. "BKT 1") — skipped
        c.setPos(getCellBigDecimal(row, COL_POS));
        c.setCaseValue(getCellBigDecimal(row, COL_CASE_VALUE));
        c.setLmFri(getCellBigDecimal(row, COL_LM_FRI));
        c.setCurFri(getCellBigDecimal(row, COL_CUR_FRI));
        c.setOdWithoutFri(getCellBigDecimal(row, COL_OD_WITHOUT_FRI));
        c.setOdWithFri(getCellBigDecimal(row, COL_OD_WITH_FRI));
        c.setEmi(getCellString(row, COL_EMI));
        c.setMCollectId(getCellString(row, COL_M_COLLECT_ID));
        c.setAddress(getCellString(row, COL_ADDRESS));
        c.setAssetName(getCellString(row, COL_ASSET_NAME));
        c.setRegistrationNumber(getCellString(row, COL_REG_NO));
        c.setEngineNo(getCellString(row, COL_ENGINE_NO));
        c.setChassisNo(getCellString(row, COL_CHASSIS_NO));
        return c;
    }

    private Case mapCsvRowToCase(String[] cols) {
        if (cols == null || cols.length < 21) return null;
        Case c = new Case();
        c.setSrNo(parseIntSafe(cols[COL_SR_NO]));
        c.setApac(cols[COL_APAC].trim());
        c.setPartyName(cols[COL_PARTY_NAME].trim());
        String phone = cols.length > COL_CUSTOMER_CONTACT ? cols[COL_CUSTOMER_CONTACT].trim() : "";
        c.setCustomerContactNo(!phone.isEmpty() ? phone : cols[COL_CUSTOMER_CONTACT_ID].trim());
        c.setFos(cols[COL_FOS].trim());
        c.setFosContactNo(cols[COL_FOS_CONTACT].trim());
        c.setHoStatus(parseStatus(cols[COL_HO_STATUS].trim()));
        c.setBkt(cols[COL_BKT].trim());
        // col[9] = bkt_label — skipped
        c.setPos(parseBigDecimalSafe(cols[COL_POS]));
        c.setCaseValue(parseBigDecimalSafe(cols[COL_CASE_VALUE]));
        c.setLmFri(parseBigDecimalSafe(cols[COL_LM_FRI]));
        c.setCurFri(parseBigDecimalSafe(cols[COL_CUR_FRI]));
        c.setOdWithoutFri(parseBigDecimalSafe(cols[COL_OD_WITHOUT_FRI]));
        c.setOdWithFri(parseBigDecimalSafe(cols[COL_OD_WITH_FRI]));
        c.setEmi(cols.length > COL_EMI ? cols[COL_EMI].trim() : null);
        c.setMCollectId(cols.length > COL_M_COLLECT_ID ? cols[COL_M_COLLECT_ID].trim() : null);
        c.setAddress(cols.length > COL_ADDRESS ? cols[COL_ADDRESS].trim() : null);
        c.setAssetName(cols.length > COL_ASSET_NAME ? cols[COL_ASSET_NAME].trim() : null);
        c.setRegistrationNumber(cols.length > COL_REG_NO ? cols[COL_REG_NO].trim() : null);
        if (cols.length > COL_ENGINE_NO) c.setEngineNo(cols[COL_ENGINE_NO].trim());
        if (cols.length > COL_CHASSIS_NO) c.setChassisNo(cols[COL_CHASSIS_NO].trim());
        return c;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private Integer getCellInt(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(cell.getStringCellValue().trim()); } catch (Exception e) { return null; }
    }

    private BigDecimal getCellBigDecimal(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC)
                return BigDecimal.valueOf(cell.getNumericCellValue());
            return new BigDecimal(cell.getStringCellValue().trim());
        } catch (Exception e) { return null; }
    }

    private CaseStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try { return CaseStatus.valueOf(value.toUpperCase().trim()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private Integer parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private BigDecimal parseBigDecimalSafe(String s) {
        try { return new BigDecimal(s.trim()); } catch (Exception e) { return null; }
    }

    private String nullSafe(String s) { return s != null ? s : ""; }
}
