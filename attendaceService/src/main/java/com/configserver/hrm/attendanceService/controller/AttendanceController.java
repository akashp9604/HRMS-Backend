package com.configserver.hrm.attendanceService.controller;

import com.configserver.hrm.attendanceService.dto.AttendanceRequestDTO;
import com.configserver.hrm.attendanceService.dto.AttendanceSummaryDTO;
import com.configserver.hrm.attendanceService.dto.DailySummaryDTO;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import com.configserver.hrm.attendanceService.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:3000")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    // ✅ Manual import (POST JSON body with validation)
    @PostMapping("/import")
    public ResponseEntity<String> importAttendance(@RequestBody @Valid List<AttendanceRequestDTO> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) {
            return ResponseEntity.badRequest().body("Attendance list cannot be empty");
        }
        attendanceService.importAttendance(attendanceList);
        return ResponseEntity.ok("Attendance Imported Successfully");
    }

    // ✅ Daily import (today) - return Entity instead of DTO
    @PostMapping("/import/daily")
    public ResponseEntity<List<EmployeeAttendance>> importTodayAttendance() {
        List<EmployeeAttendance> importedData = attendanceService.importDailyAttendanceFromEtimeOffice();
        return ResponseEntity.ok(importedData.isEmpty() ? Collections.emptyList() : importedData);
    }

    // ✅ Import by custom date - return Entity instead of DTO
    @PostMapping("/import/by-date")
    public ResponseEntity<List<EmployeeAttendance>> importAttendanceByDate(@RequestParam String date) {
        LocalDate reportDate;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            reportDate = LocalDate.parse(date, formatter);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        List<EmployeeAttendance> importedData = attendanceService.importAttendanceFromEtimeOffice(reportDate);
        return ResponseEntity.ok(importedData.isEmpty() ? Collections.emptyList() : importedData);
    }

   // ✅ Download Attendance Report API
   @GetMapping("/download")
   public ResponseEntity<ByteArrayResource> downloadAttendanceReport(@RequestParam String date) {
       LocalDate reportDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

       byte[] reportData = attendanceService.downloadAttendanceReport(reportDate);

       ByteArrayResource resource = new ByteArrayResource(reportData);

       HttpHeaders headers = new HttpHeaders();
       headers.setContentType(MediaType.parseMediaType(
               "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
       headers.setContentDisposition(ContentDisposition.attachment()
               .filename("Attendance_Report_" + reportDate + ".xlsx")
               .build());

       return ResponseEntity.ok()
               .headers(headers)
               .body(resource);
   }
    @GetMapping("/employees-info")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesInfo() {
        List<Map<String, Object>> employees = attendanceService.getEmployeesFromAttendance();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/download/monthly")
    public ResponseEntity<byte[]> downloadMonthlyReport(
            @RequestParam String monthYear,
            @RequestParam List<String> employeeIds) {

        try {
            byte[] excelData = attendanceService.downloadMonthlyReport(monthYear, employeeIds);

            if (excelData == null || excelData.length == 0) {
                return ResponseEntity.noContent().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String fileName = "MonthlyReport_" + monthYear.replace(" ", "_") + ".xlsx";
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(fileName)
                    .build());

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .body(("Error while generating report: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/download/monthly/json")
    public ResponseEntity<Map<String, Object>> downloadMonthlyReportJson(
            @RequestParam String monthYear,
            @RequestParam List<String> employeeIds) {

        Map<String, Object> reportJson = attendanceService.downloadMonthlyReportAsJson(monthYear, employeeIds);
        return ResponseEntity.ok(reportJson);
    }
    @GetMapping("/present-absent-summary")
    public ResponseEntity<DailySummaryDTO> getPresentAbsentSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date == null) {
            date = LocalDate.now(); // default to today
        }

        return ResponseEntity.ok(attendanceService.getPresentAbsentSummary(date));
    }
    @PostMapping("/import/smart-monthly/{monthYear}")
    public ResponseEntity<List<EmployeeAttendance>> smartMonthlyImport(
            @PathVariable String monthYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate companyStartDate
    ) {
        try {
            // Default company start date if not passed
            if (companyStartDate == null) {
                companyStartDate = LocalDate.of(2025, 1, 1);
            }

            List<EmployeeAttendance> result = attendanceService.importSmartMonthlyAttendance(monthYear, companyStartDate);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
    @GetMapping("/summary/{monthYear}")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @PathVariable String monthYear,
            @RequestParam String employeeId) {
        try {
            Map<String, Object> summary = attendanceService.getMonthlySummaryForEmployee(employeeId, monthYear);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/import/smart-monthly")
    public ResponseEntity<?> getAllImportedAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<EmployeeAttendance> records = attendanceService.getAllImportedAttendance(startDate, endDate);
        return ResponseEntity.ok(records);
    }
    @GetMapping("/employee/{employeeId}/monthly-summary")
    public ResponseEntity<Map<String, Object>> getEmployeeMonthlySummary(
            @PathVariable String employeeId,
            @RequestParam String month) {
        try {
            Map<String, Object> summary = attendanceService.getMonthlySummaryForEmployee(employeeId, month);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/employee-monthly-details")
    public ResponseEntity<List<EmployeeAttendance>> employeeMonthlyDetails(
            @RequestParam String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<EmployeeAttendance> records = attendanceService.employeeMonthlyDetails(employeeId, startDate, endDate);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/employee/{employeeId}/daily")
    public ResponseEntity<?> getEmployeeDailyAttendance(
            @PathVariable String employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            // if no date provided, use today
            if (date == null) {
                date = LocalDate.now();
            }

            EmployeeAttendance attendance = attendanceService.getAttendanceForEmployeeOnDate(employeeId, date);

            if (attendance != null) {
                return ResponseEntity.ok(attendance);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No attendance record found for " + date);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

}


