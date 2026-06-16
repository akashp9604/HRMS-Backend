package com.configserver.hrm.attendanceService.controller;

import com.configserver.hrm.attendanceService.dto.AttendanceRequestDTO;
import com.configserver.hrm.attendanceService.dto.DailySummaryDTO;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import com.configserver.hrm.attendanceService.repository.EmployeeAttendanceRepository;
import com.configserver.hrm.attendanceService.service.AttendanceService;
import com.configserver.hrm.attendanceService.service.EtimeOfficeExcelParser;
import com.configserver.hrm.attendanceService.service.LocalFileImportService;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:3000")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EtimeOfficeExcelParser etimeOfficeExcelParser;

    @Autowired
    private EmployeeAttendanceRepository repository;

    @Autowired
    private LocalFileImportService localFileImportService;

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

    /*
        Get data for Attendance Management - Current
    */
    @GetMapping("/daily/{date}")
    public ResponseEntity<List<EmployeeAttendance>> getDailyAttendance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<EmployeeAttendance> records = attendanceService.getDailyAttendance(date);
        return ResponseEntity.ok(records != null ? records : Collections.emptyList());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<EmployeeAttendance>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<EmployeeAttendance> records = attendanceService.getDailyAttendance(date);
        return ResponseEntity.ok(records != null ? records : Collections.emptyList());
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

    /**
     * Import monthly attendance from eTimeOffice Excel report
     */
    @PostMapping(value = "/import/etime-monthly", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importEtimeMonthlyReport(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "excelFile", required = false) MultipartFile excelFile,
            @RequestParam(required = false, defaultValue = "ETIME_MONTHLY") String sourceType) {

        // Try to get file from either parameter name
        MultipartFile actualFile = file != null ? file : excelFile;

        try {
            // Log request details for debugging
            System.out.println("=== Debug Information ===");
            System.out.println("File parameter (file): " + (file != null ? file.getOriginalFilename() : "null"));
            System.out.println("File parameter (excelFile): " + (excelFile != null ? excelFile.getOriginalFilename() : "null"));
            System.out.println("Actual file: " + (actualFile != null ? actualFile.getOriginalFilename() : "null"));
            System.out.println("Source type: " + sourceType);

            if (actualFile == null || actualFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No file uploaded. Please upload a file with parameter name 'file' or 'excelFile'",
                        "receivedParams", Map.of("file", file != null, "excelFile", excelFile != null)
                ));
            }

            String fileName = actualFile.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Please upload a valid Excel file (.xlsx or .xls). Uploaded file: " + fileName
                ));
            }

            // ✅ Call the service method that actually saves to database
            List<EmployeeAttendance> importedData = attendanceService.importEtimeMonthlyReport(actualFile, sourceType);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Monthly attendance imported successfully from eTimeOffice report");
            response.put("recordsImported", importedData.size());
            response.put("employeesProcessed", importedData.stream().map(EmployeeAttendance::getEmployeeId).distinct().count());
            response.put("data", importedData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error importing Excel file: " + e.getMessage()));
        }
    }


    /**
     * Import from local file path (calls the existing API internally)
     */
    @PostMapping("/import/from-local-path")
    public ResponseEntity<?> importFromLocalPath(
            @RequestParam(defaultValue = "F:/ConfigServerLlp/HRMS-Backend/attendaceService/src/monthperformance01062026185649.xls")
            String filePath,
            @RequestParam(required = false, defaultValue = "ETIME_MONTHLY")
            String sourceType) {

        try {
            System.out.println("=== Importing from local file path ===");
            System.out.println("File path: " + filePath);

            // Method 1: Using RestTemplate (calls your existing API)
            // Map<String, Object> result = localFileImportService.importViaRestTemplate(filePath, sourceType);

            // Method 2: Direct call (recommended - no HTTP overhead)
            Map<String, Object> result = localFileImportService.importDirectly(filePath, sourceType);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Quick import using default file path
     */
    @PostMapping("/import/auto")
    public ResponseEntity<?> autoImport() {
        String defaultPath = "F:/ConfigServerLlp/HRMS-Backend/attendaceService/src/monthperformance01062026185649.xls";

        try {
            Map<String, Object> result = localFileImportService.importDirectly(defaultPath, "ETIME_MONTHLY");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}


