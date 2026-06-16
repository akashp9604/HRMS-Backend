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

    public List<EmployeeAttendance> parseMonthlyReport(MultipartFile file, String sourceType) throws Exception {
        List<EmployeeAttendance> allAttendance = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // Extract month/year from the report
            YearMonth yearMonth = extractYearMonthFromSheet(sheet, evaluator);
            System.out.println("Processing report for: " + yearMonth);

            int lastRowNum = sheet.getLastRowNum();
            int currentRow = 0;

            while (currentRow <= lastRowNum) {
                Row row = sheet.getRow(currentRow);
                if (row == null) {
                    currentRow++;
                    continue;
                }

                // Look for "Empcode" pattern in column A
                String firstCellValue = getCellValue(row.getCell(0), evaluator);
                if ("Empcode".equalsIgnoreCase(firstCellValue)) {
                    System.out.println("Found employee block at row: " + currentRow);

                    // Parse employee from this row
                    EmployeeBlock block = parseEmployeeBlock(sheet, currentRow, evaluator, yearMonth);

                    if (block != null && block.employeeId != null && !block.attendances.isEmpty()) {
                        System.out.println("Processing employee: " + block.employeeId + " - " + block.employeeName);
                        System.out.println("Attendance records found: " + block.attendances.size());

                        for (EmployeeAttendance attendance : block.attendances) {
                            attendance.setSourceType(sourceType != null ? sourceType : "ETIME_MONTHLY");
                            allAttendance.add(attendance);
                        }
                    }

                    // Move to next employee block (each block is exactly 10 rows)
                    currentRow = currentRow + 10;
                    continue;
                }
                currentRow++;
            }
        }

        System.out.println("Total attendance records parsed: " + allAttendance.size());
        return allAttendance;
    }

    private YearMonth extractYearMonthFromSheet(Sheet sheet, FormulaEvaluator evaluator) {
        for (int i = 0; i <= 5; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int col = 0; col <= 30; col++) {
                String cellValue = getCellValue(row.getCell(col), evaluator);
                if (cellValue != null && cellValue.contains("Report Month")) {
                    String monthValue = getCellValue(row.getCell(col + 3), evaluator);
                    if (monthValue != null && !monthValue.isEmpty()) {
                        System.out.println("Found Report Month: " + monthValue);
                        return parseMonthYear(monthValue);
                    }
                }
            }
        }
        return YearMonth.now();
    }

    private EmployeeBlock parseEmployeeBlock(Sheet sheet, int empCodeRowIndex, FormulaEvaluator evaluator, YearMonth yearMonth) {
        EmployeeBlock block = new EmployeeBlock();

        try {
            Row empCodeRow = sheet.getRow(empCodeRowIndex);
            if (empCodeRow == null) return null;

            // Employee ID at column C (index 2)
            block.employeeId = getCellValue(empCodeRow.getCell(2), evaluator);
            if (block.employeeId == null || block.employeeId.isEmpty()) return null;
            block.employeeId = block.employeeId.trim();

            // Employee name at column H (index 7)
            block.employeeName = getCellValue(empCodeRow.getCell(7), evaluator);
            if (block.employeeName == null || block.employeeName.isEmpty()) {
                block.employeeName = block.employeeId;
            } else {
                block.employeeName = block.employeeName.trim();
            }

            System.out.println("Found employee: ID=" + block.employeeId + ", Name=" + block.employeeName);

            // CORRECTED ROW OFFSETS based on Excel structure:
            // Row empCodeRowIndex + 0: Empcode row
            // Row empCodeRowIndex + 1: Day numbers (1,2,3...)
            // Row empCodeRowIndex + 2: Weekday names (Mon, Tue, Wed...)
            // Row empCodeRowIndex + 3: IN times
            // Row empCodeRowIndex + 4: OUT times
            // Row empCodeRowIndex + 5: WORK hours
            // Row empCodeRowIndex + 6: Break
            // Row empCodeRowIndex + 7: OT
            // Row empCodeRowIndex + 8: Status

            int dayNumbersRowIndex = empCodeRowIndex + 1;  // FIXED: +1 instead of +2
            int inTimesRowIndex = empCodeRowIndex + 3;     // FIXED: +3 instead of +4
            int outTimesRowIndex = empCodeRowIndex + 4;    // FIXED: +4 instead of +5
            int statusRowIndex = empCodeRowIndex + 8;      // FIXED: +8 instead of +9

            Row dayNumbersRow = sheet.getRow(dayNumbersRowIndex);
            Row inTimesRow = sheet.getRow(inTimesRowIndex);
            Row outTimesRow = sheet.getRow(outTimesRowIndex);
            Row statusRow = sheet.getRow(statusRowIndex);

            System.out.println("Day numbers row index: " + dayNumbersRowIndex);
            System.out.println("IN times row index: " + inTimesRowIndex);
            System.out.println("OUT times row index: " + outTimesRowIndex);
            System.out.println("Status row index: " + statusRowIndex);

            if (dayNumbersRow == null) {
                System.out.println("Day numbers row not found at index: " + dayNumbersRowIndex);
                return block;
            }

            if (statusRow == null) {
                System.out.println("Status row not found at index: " + statusRowIndex);
                return block;
            }

            // Find where day numbers start (column containing "1")
            int startCol = findStartColumnForDayNumbers(dayNumbersRow, evaluator);
            if (startCol == -1) {
                System.out.println("Could not find day numbers starting column");
                return block;
            }

            System.out.println("Day numbers start at column: " + startCol);

            // Debug: Print first few day numbers (should be 1,2,3,4,5...)
            for (int i = 0; i < 5; i++) {
                String dayVal = getCellValue(dayNumbersRow.getCell(startCol + i), evaluator);
                System.out.println("Day " + (i+1) + " column value: " + dayVal);
            }

            // Parse each day of the month
            int daysInMonth = yearMonth.lengthOfMonth();
            int parsedCount = 0;

            for (int day = 1; day <= daysInMonth; day++) {
                int colIndex = startCol + (day - 1);

                // Verify this column contains the correct day number
                String dayNumberStr = getCellValue(dayNumbersRow.getCell(colIndex), evaluator);
                if (dayNumberStr == null || dayNumberStr.isEmpty()) {
                    continue;
                }

                try {
                    int dayNum = Integer.parseInt(dayNumberStr.trim());
                    if (dayNum != day) {
                        colIndex = findColumnForDay(dayNumbersRow, day, evaluator, startCol);
                        if (colIndex == -1) {
                            continue;
                        }
                    }
                } catch (NumberFormatException e) {
                    continue;
                }

                LocalDate date = yearMonth.atDay(day);

                // Get status
                String status = getCellValue(statusRow.getCell(colIndex), evaluator);
                if (status == null || status.isEmpty()) {
                    continue;
                }
                status = status.trim().toUpperCase();

                // Get IN and OUT times
                String inTimeStr = normalizeTime(getCellValue(inTimesRow.getCell(colIndex), evaluator));
                String outTimeStr = normalizeTime(getCellValue(outTimesRow.getCell(colIndex), evaluator));

                // Create attendance record
                EmployeeAttendance attendance = createAttendanceRecord(
                        block, date, inTimeStr, outTimeStr, status
                );

                block.attendances.add(attendance);
                parsedCount++;
            }

            System.out.println("Parsed " + parsedCount + " records for employee: " + block.employeeId);

        } catch (Exception e) {
            System.err.println("Error parsing employee block: " + e.getMessage());
            e.printStackTrace();
        }

        return block;
    }

    private int findStartColumnForDayNumbers(Row dayNumbersRow, FormulaEvaluator evaluator) {
        // Look for cell containing "1" in columns 1-10 (B to K)
        for (int col = 1; col <= 10; col++) {
            Cell cell = dayNumbersRow.getCell(col);
            if (cell != null) {
                String val = getCellValue(cell, evaluator);
                if (val != null && val.trim().equals("1")) {
                    return col;
                }
            }
        }
        return 1;
    }

    private int findColumnForDay(Row dayRow, int targetDay, FormulaEvaluator evaluator, int startCol) {
        for (int col = startCol; col <= 35; col++) {
            Cell cell = dayRow.getCell(col);
            if (cell != null) {
                String val = getCellValue(cell, evaluator);
                if (val != null && !val.isEmpty()) {
                    try {
                        int dayNum = Integer.parseInt(val.trim());
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

    private EmployeeAttendance createAttendanceRecord(EmployeeBlock block, LocalDate date,
                                                      String inTimeStr, String outTimeStr,
                                                      String statusCode) {
        EmployeeAttendance attendance = new EmployeeAttendance();
        attendance.setEmployeeId(block.employeeId);
        attendance.setEmployeeName(block.employeeName);
        attendance.setDate(date);
        attendance.setShift("Day");

        attendance.setLateIn("0:00");
        attendance.setErlOut("0:00");
        attendance.setOverTime("0:00");

        LocalTime inTime = parseTime(inTimeStr);
        LocalTime outTime = parseTime(outTimeStr);
        attendance.setInTime(inTime);
        attendance.setOutTime(outTime);

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
                return AttendanceStatus.PRESENT;
            case "WO":
                return AttendanceStatus.WEEK_OFF;
            case "A":
                return AttendanceStatus.ABSENT;
            case "HL":
                return AttendanceStatus.HALF_DAY;
            case "LV":
                return AttendanceStatus.LEAVE;
            default:
                return AttendanceStatus.ABSENT;
        }
    }

    private String mapStatusToRemark(String status) {
        if (status == null) return "";

        switch (status.toUpperCase()) {
            case "P":
                return "Present";
            case "A":
                return "Absent";
            case "WO":
                return "Week Off";
            case "HL":
                return "Half Day";
            case "LV":
                return "Leave";
            default:
                return status;
        }
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
                if (hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59) {
                    return LocalTime.of(hours, minutes);
                }
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
                if (numValue < 1 && numValue > 0) {
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

    private static class EmployeeBlock {
        String employeeId;
        String employeeName;
        List<EmployeeAttendance> attendances = new ArrayList<>();
    }
}