package com.configserver.hrm.attendanceService.service;

import com.configserver.hrm.attendanceService.dto.AttendanceRequestDTO;
import com.configserver.hrm.attendanceService.dto.AttendanceSummaryDTO;
import com.configserver.hrm.attendanceService.dto.DailySummaryDTO;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    void importAttendance(List<AttendanceRequestDTO> attendanceList);

    // ✅ Today's daily attendance (returns saved entity objects)
    List<EmployeeAttendance> importDailyAttendanceFromEtimeOffice();

    // ✅ Custom date attendance (returns saved entity objects)
    List<EmployeeAttendance> importAttendanceFromEtimeOffice(LocalDate date);

    // ✅ Get all attendance for a date
    List<EmployeeAttendance> getDailyAttendance(LocalDate date);

    // ✅ Get employee attendance between dates
    List<EmployeeAttendance> getEmployeeAttendance(String employeeId, LocalDate from, LocalDate to);

    // ✅ Download attendance report (Excel bytes)
    byte[] downloadAttendanceReport(LocalDate date);
    // Download monthly attendance report (PDF bytes)
    byte[] downloadMonthlyReport(String monthYear, List<String> employeeIds);

    List<Map<String, Object>> getEmployeesFromAttendance();
    AttendanceSummaryDTO getMonthlyAttendanceSummary(String employeeId, int month, int year);

    // Return parsed monthly PDF report as structured JSON map
    Map<String, Object> downloadMonthlyReportAsJson(String monthYear, List<String> employeeIds);

    DailySummaryDTO getPresentAbsentSummary(LocalDate date);

    List<EmployeeAttendance> importSmartMonthlyAttendance(String monthYear, LocalDate companyStartDate);

    //Map<String, Object> getMonthlySummaryForEmployee(String employeeId, String monthYear);

    List<EmployeeAttendance> getAllImportedAttendance(LocalDate startDate, LocalDate endDate);

    List<EmployeeAttendance> getAttendanceByEmployeeAndDuration(String employeeId, LocalDate startDate, LocalDate endDate);

    Map<String, Object> getMonthlySummaryForEmployee(String employeeId, String month);

    List<EmployeeAttendance> employeeMonthlyDetails(String employeeId, LocalDate startDate, LocalDate endDate);

    EmployeeAttendance getAttendanceForEmployeeOnDate(String employeeId, LocalDate date);

}
