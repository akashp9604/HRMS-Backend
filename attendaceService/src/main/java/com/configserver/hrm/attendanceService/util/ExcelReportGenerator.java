package com.configserver.hrm.attendanceService.util;

import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ExcelReportGenerator {

    public static byte[] generateAttendanceReport(List<EmployeeAttendance> attendanceList) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Report");

        // Header Row
        String[] headers = {"Employee ID", "Employee Name", "Shift", "Date", "In Time", "Out Time", "Late In", "Early Out", "Work Hours", "Overtime", "Status", "Remark"};
        Row headerRow = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowIdx = 1;
        for (EmployeeAttendance record : attendanceList) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(record.getEmployeeId());
            row.createCell(1).setCellValue(record.getEmployeeName());
            row.createCell(2).setCellValue(record.getShift());
            row.createCell(3).setCellValue(record.getDate().toString());
            row.createCell(4).setCellValue(record.getInTime() != null ? record.getInTime().toString() : "");
            row.createCell(5).setCellValue(record.getOutTime() != null ? record.getOutTime().toString() : "");
            row.createCell(6).setCellValue(record.getLateIn());
            row.createCell(7).setCellValue(record.getErlOut());
            row.createCell(8).setCellValue(record.getWorkHours());
            row.createCell(9).setCellValue(record.getOverTime());
            row.createCell(10).setCellValue(record.getStatus().toString());
            row.createCell(11).setCellValue(record.getRemark());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }
}
