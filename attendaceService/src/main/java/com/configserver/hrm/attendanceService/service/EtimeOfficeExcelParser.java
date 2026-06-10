package com.configserver.hrm.attendanceService.service;

import com.configserver.hrm.attendanceService.entity.AttendanceStatus;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EtimeOfficeExcelParser {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM-yyyy", Locale.ENGLISH);

    /**
     * Parse eTimeOffice monthly report Excel file - ONLY parses, does NOT save to DB
     */
    public List<EmployeeAttendance> parseMonthlyReport(MultipartFile file, String sourceType) throws Exception {
        List<EmployeeAttendance> allAttendance = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            int lastRowNum = sheet.getLastRowNum();
            int currentRow = 0;

            // Get month/year from the first occurrence
            String monthYear = extractMonthYear(sheet, evaluator);
            YearMonth yearMonth = parseMonthYear(monthYear);

            System.out.println("Processing report for: " + yearMonth);

            while (currentRow <= lastRowNum) {
                Row row = sheet.getRow(currentRow);
                if (row == null) {
                    currentRow++;
                    continue;
                }

                // Look for "Empcode" pattern - indicates start of employee block
                String firstCellValue = getCellValue(row.getCell(0), evaluator);
                if ("Empcode".equalsIgnoreCase(firstCellValue)) {
                    System.out.println("Found employee block at row: " + currentRow);

                    // Parse employee block starting from this row
                    EmployeeBlock block = parseEmployeeBlock(sheet, currentRow, evaluator, yearMonth);

                    if (block != null && block.employeeId != null && !block.attendances.isEmpty()) {
                        System.out.println("Processing employee: " + block.employeeId + " - " + block.employeeName);
                        System.out.println("Attendance records found: " + block.attendances.size());

                        // Add source type to all attendance records
                        for (EmployeeAttendance attendance : block.attendances) {
                            attendance.setSourceType(sourceType != null ? sourceType : "ETIME_MONTHLY");
                            allAttendance.add(attendance);
                        }

                        System.out.println("Added " + block.attendances.size() + " records for employee: " + block.employeeId);
                    }

                    // Skip to next employee block - look for next "Empcode"
                    currentRow = findNextEmpcodeRow(sheet, currentRow + 1, evaluator);
                    if (currentRow == -1) {
                        break; // No more employee blocks
                    }
                    continue;
                }
                currentRow++;
            }
        }

        System.out.println("Total attendance records parsed: " + allAttendance.size());
        return allAttendance;
    }

    /**
     * Find the next row containing "Empcode" starting from given row
     */
    private int findNextEmpcodeRow(Sheet sheet, int startRow, FormulaEvaluator evaluator) {
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                String cellValue = getCellValue(row.getCell(0), evaluator);
                if ("Empcode".equalsIgnoreCase(cellValue)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Parse a single employee block from the Excel
     */
    private EmployeeBlock parseEmployeeBlock(Sheet sheet, int startRow, FormulaEvaluator evaluator, YearMonth yearMonth) {
        EmployeeBlock block = new EmployeeBlock();

        try {
            // Row 0 of block: "Empcode" | (blank) | {employeeId} | (blank) | "Name" | (blank) | {employeeName}
            Row empCodeRow = sheet.getRow(startRow);
            if (empCodeRow == null) return null;

            // Get employee ID from column C (index 2)
            block.employeeId = getCellValue(empCodeRow.getCell(2), evaluator);
            if (block.employeeId == null || block.employeeId.isEmpty()) return null;
            block.employeeId = block.employeeId.trim();

            // Get employee name - try different possible columns
            block.employeeName = getCellValue(empCodeRow.getCell(7), evaluator); // Column H
            if (block.employeeName == null || block.employeeName.isEmpty()) {
                block.employeeName = getCellValue(empCodeRow.getCell(6), evaluator); // Column G
            }
            if (block.employeeName == null || block.employeeName.isEmpty()) {
                block.employeeName = getCellValue(empCodeRow.getCell(5), evaluator); // Column F
            }
            if (block.employeeName != null) {
                block.employeeName = block.employeeName.trim();
            } else {
                block.employeeName = block.employeeId; // Use ID as fallback
            }

            System.out.println("Found employee: ID=" + block.employeeId + ", Name=" + block.employeeName);

            // Find the row where day numbers start
            int dataStartRow = findDataStartRow(sheet, startRow, evaluator);
            if (dataStartRow != -1) {
                block.attendances = parseDailyData(sheet, dataStartRow, block, evaluator, yearMonth);
            } else {
                System.out.println("Warning: Could not find data start row for employee: " + block.employeeId);
            }

        } catch (Exception e) {
            System.err.println("Error parsing employee block at row " + startRow + ": " + e.getMessage());
            e.printStackTrace();
        }

        return block;
    }

    /**
     * Find the row where day columns start (1,2,3...31)
     */
    private int findDataStartRow(Sheet sheet, int startRow, FormulaEvaluator evaluator) {
        // Look within 20 rows after startRow
        for (int i = startRow + 2; i <= startRow + 20 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // Check column B (index 1) for day number "1"
            Cell cellB = row.getCell(1);
            if (cellB != null) {
                String val = getCellValue(cellB, evaluator);
                if (val != null && val.equals("1")) {
                    System.out.println("Found data start row at: " + i);
                    return i;
                }
            }

            // Also check column C (index 2) if B doesn't have "1"
            Cell cellC = row.getCell(2);
            if (cellC != null) {
                String val = getCellValue(cellC, evaluator);
                if (val != null && val.equals("1")) {
                    System.out.println("Found data start row at: " + i);
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Find the starting column where day numbers begin
     */
    private int findStartColumn(Row dayRow, FormulaEvaluator evaluator) {
        for (int col = 0; col <= 10; col++) {
            Cell cell = dayRow.getCell(col);
            if (cell != null) {
                String val = getCellValue(cell, evaluator);
                if (val != null && val.equals("1")) {
                    return col;
                }
            }
        }
        return 1; // Default to column B (index 1)
    }

    /**
     * Parse daily IN/OUT times and status for an employee
     */
    private List<EmployeeAttendance> parseDailyData(Sheet sheet, int dataStartRow,
                                                    EmployeeBlock block,
                                                    FormulaEvaluator evaluator,
                                                    YearMonth yearMonth) {
        List<EmployeeAttendance> attendances = new ArrayList<>();

        try {
            Row dayNumbersRow = sheet.getRow(dataStartRow);      // Day numbers (1-31)
            Row inTimesRow = sheet.getRow(dataStartRow + 2);     // IN times
            Row outTimesRow = sheet.getRow(dataStartRow + 3);    // OUT times
            Row statusRow = sheet.getRow(dataStartRow + 7);      // Status row

            if (dayNumbersRow == null) {
                System.out.println("Day numbers row is null at row: " + dataStartRow);
                return attendances;
            }

            // Try to find status row if not found at expected offset
            if (statusRow == null) {
                // Try alternative offsets
                for (int offset = 5; offset <= 8; offset++) {
                    Row testRow = sheet.getRow(dataStartRow + offset);
                    if (testRow != null) {
                        String testVal = getCellValue(testRow.getCell(0), evaluator);
                        if ("Status".equalsIgnoreCase(testVal)) {
                            statusRow = testRow;
                            System.out.println("Found Status row at offset +" + offset);
                            break;
                        }
                    }
                }
            }

            if (statusRow == null) {
                System.out.println("Could not find Status row for employee: " + block.employeeId);
                return attendances;
            }

            // Find the start column for day numbers
            int startCol = findStartColumn(dayNumbersRow, evaluator);
            System.out.println("Start column for days: " + startCol);

            // Process each day of the month
            for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
                int colIndex = startCol + (day - 1);

                // Get day number to verify
                String dayNumberStr = getCellValue(dayNumbersRow.getCell(colIndex), evaluator);
                if (dayNumberStr == null || dayNumberStr.isEmpty()) {
                    continue;
                }

                try {
                    int dayNum = Integer.parseInt(dayNumberStr.trim());
                    if (dayNum != day) {
                        // Try to find correct column for this day
                        colIndex = findColumnIndexForDay(dayNumbersRow, day, evaluator, startCol);
                        if (colIndex == -1) {
                            continue;
                        }
                    }
                } catch (NumberFormatException e) {
                    continue;
                }

                LocalDate date = yearMonth.atDay(day);

                // Get status for this day
                String status = getCellValue(statusRow.getCell(colIndex), evaluator);
                if (status == null || status.isEmpty()) {
                    continue;
                }

                status = status.trim().toUpperCase();

                // Get IN and OUT times
                String inTimeStr = normalizeTime(getCellValue(inTimesRow.getCell(colIndex), evaluator));
                String outTimeStr = normalizeTime(getCellValue(outTimesRow.getCell(colIndex), evaluator));

                // Create attendance record
                EmployeeAttendance attendance = createAttendanceRecordForDay(
                        block, date, inTimeStr, outTimeStr, status
                );

                attendances.add(attendance);
            }

            System.out.println("Parsed " + attendances.size() + " attendance records for employee: " + block.employeeId);

        } catch (Exception e) {
            System.err.println("Error parsing daily data for employee " + block.employeeId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Find which column index contains the specified day number
     */
    private int findColumnIndexForDay(Row dayRow, int targetDay, FormulaEvaluator evaluator, int startCol) {
        for (int col = startCol; col <= 33; col++) {
            Cell cell = dayRow.getCell(col);
            if (cell != null) {
                String cellValue = getCellValue(cell, evaluator);
                if (cellValue != null && !cellValue.isEmpty()) {
                    try {
                        int dayNum = Integer.parseInt(cellValue.trim());
                        if (dayNum == targetDay) {
                            return col;
                        }
                    } catch (NumberFormatException e) {
                        // Not a number, continue
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Create EmployeeAttendance record for a specific day
     */
    private EmployeeAttendance createAttendanceRecordForDay(EmployeeBlock block, LocalDate date,
                                                            String inTimeStr, String outTimeStr,
                                                            String statusCode) {
        EmployeeAttendance attendance = new EmployeeAttendance();
        attendance.setEmployeeId(block.employeeId);
        attendance.setEmployeeName(block.employeeName);
        attendance.setDate(date);
        attendance.setShift("Day");

        // Set default values
        attendance.setLateIn("0:00");
        attendance.setErlOut("0:00");
        attendance.setOverTime("0:00");

        // Parse times
        LocalTime inTime = parseTime(inTimeStr);
        LocalTime outTime = parseTime(outTimeStr);
        attendance.setInTime(inTime);
        attendance.setOutTime(outTime);

        // Calculate work hours
        double workHours = 0.0;
        if (inTime != null && outTime != null) {
            try {
                workHours = java.time.Duration.between(inTime, outTime).toMinutes() / 60.0;
                workHours = Math.round(workHours * 100.0) / 100.0;
            } catch (Exception e) {
                workHours = 0.0;
            }
        }
        attendance.setWorkHours(workHours);

        // Map status code to AttendanceStatus
        AttendanceStatus attendanceStatus = mapStatusToEnum(statusCode);
        attendance.setStatus(attendanceStatus);
        attendance.setRemark(mapStatusToRemark(statusCode));
        attendance.setPunchOutEmailSent(false);

        return attendance;
    }

    private AttendanceStatus mapStatusToEnum(String status) {
        if (status == null) return AttendanceStatus.ABSENT;

        switch (status.toUpperCase()) {
            case "P":
            case "PRESENT":
                return AttendanceStatus.PRESENT;
            case "WO":
                return AttendanceStatus.PRESENT; // Work from Office - considered present
            case "A":
            case "ABSENT":
                return AttendanceStatus.ABSENT;
            case "HL":
            case "HALF_DAY":
                return AttendanceStatus.HALF_DAY;
            case "LV":
            case "LEAVE":
                return AttendanceStatus.LEAVE;
            default:
                return AttendanceStatus.ABSENT;
        }
    }

    private String mapStatusToRemark(String status) {
        if (status == null) return "";

        switch (status.toUpperCase()) {
            case "P": return "Present";
            case "A": return "Absent";
            case "WO": return "Work from Office";
            case "HL": return "Half Day";
            case "LV": return "Leave";
            default: return status;
        }
    }

    private String extractMonthYear(Sheet sheet, FormulaEvaluator evaluator) {
        for (int i = 0; i <= Math.min(30, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int col = 0; col <= 30; col++) {
                String cellValue = getCellValue(row.getCell(col), evaluator);
                if (cellValue != null && cellValue.contains("Report Month")) {
                    String monthValue = getCellValue(row.getCell(col + 3), evaluator);
                    if (monthValue != null && !monthValue.isEmpty()) {
                        System.out.println("Found Report Month: " + monthValue);
                        return monthValue;
                    }
                }
            }
        }

        for (int i = 0; i <= Math.min(30, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int col = 0; col <= 20; col++) {
                String cellValue = getCellValue(row.getCell(col), evaluator);
                if (cellValue != null && cellValue.matches("\\w+-\\d{4}")) {
                    System.out.println("Found month-year pattern: " + cellValue);
                    return cellValue;
                }
            }
        }

        String defaultMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM-yyyy", Locale.ENGLISH));
        System.out.println("Using default month: " + defaultMonth);
        return defaultMonth;
    }

    private YearMonth parseMonthYear(String monthYear) {
        try {
            if (monthYear == null || monthYear.isEmpty()) {
                return YearMonth.now();
            }
            return YearMonth.parse(monthYear, MONTH_FORMATTER);
        } catch (Exception e) {
            System.err.println("Error parsing month-year: " + monthYear);
            return YearMonth.now();
        }
    }

    private String normalizeTime(String time) {
        if (time == null) return null;
        time = time.trim();
        if (time.isEmpty() || time.equals("--:--") || time.equals("00:00") || time.equals("0:00")) {
            return null;
        }
        if (time.length() > 5 && time.indexOf(':') > 0) {
            String[] parts = time.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1];
            }
        }
        return time;
    }

    private LocalTime parseTime(String time) {
        if (time == null) return null;
        try {
            time = time.trim();
            if (time.contains(":")) {
                String[] parts = time.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return LocalTime.of(hours, minutes);
            }
            return LocalTime.parse(time, TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                String value = cell.getStringCellValue();
                return value != null ? value.trim() : null;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        return cell.getLocalDateTimeCellValue().toLocalTime().format(TIME_FORMATTER);
                    } catch (Exception e) {
                        return cell.toString();
                    }
                }
                double numValue = cell.getNumericCellValue();
                // Handle Excel time values (fraction of day)
                if (numValue < 1 && numValue > 0) {
                    // Convert Excel time to HH:MM format
                    int totalSeconds = (int) (numValue * 24 * 3600);
                    int hours = totalSeconds / 3600;
                    int minutes = (totalSeconds % 3600) / 60;
                    return String.format("%02d:%02d", hours, minutes);
                }
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case FORMULA:
                if (evaluator != null) {
                    try {
                        CellValue evaluated = evaluator.evaluate(cell);
                        if (evaluated.getCellType() == CellType.STRING) {
                            return evaluated.getStringValue();
                        } else if (evaluated.getCellType() == CellType.NUMERIC) {
                            double val = evaluated.getNumberValue();
                            if (val == Math.floor(val)) {
                                return String.valueOf((long) val);
                            }
                            return String.valueOf(val);
                        }
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * Debug method to print Excel structure
     */
    public void debugExcelStructure(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            System.out.println("\n=== DEBUG: Excel Structure ===");
            System.out.println("Sheet name: " + sheet.getSheetName());
            System.out.println("Total rows: " + sheet.getLastRowNum());

            for (int i = 0; i <= Math.min(50, sheet.getLastRowNum()); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    System.out.print("Row " + i + ": ");
                    boolean hasData = false;
                    for (int j = 0; j <= 15; j++) {
                        String val = getCellValue(row.getCell(j), evaluator);
                        if (val != null && !val.isEmpty()) {
                            System.out.print("[" + j + ":" + val + "] ");
                            hasData = true;
                        }
                    }
                    if (hasData) {
                        System.out.println();
                    }
                }
            }
            System.out.println("=== End Debug ===\n");
        }
    }

    /**
     * Inner class to hold employee block data
     */
    private static class EmployeeBlock {
        String employeeId;
        String employeeName;
        List<EmployeeAttendance> attendances = new ArrayList<>();
    }
}