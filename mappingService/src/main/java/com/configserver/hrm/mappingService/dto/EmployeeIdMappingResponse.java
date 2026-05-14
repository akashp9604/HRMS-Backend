package com.configserver.hrm.mappingService.dto;

import java.time.LocalDateTime;

public class EmployeeIdMappingResponse {
    private Long id;
    private String attendanceEmpId;
    private String payrollEmpUuid;
    private String leaveEmpUuid;
    private String employeeServiceUuid;
    private String employeeName;
    private String employeeEmail;

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    // ✅ NEW FIELDS
    private Long annualStructureId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAttendanceEmpId() { return attendanceEmpId; }
    public void setAttendanceEmpId(String attendanceEmpId) { this.attendanceEmpId = attendanceEmpId; }

    public String getPayrollEmpUuid() { return payrollEmpUuid; }
    public void setPayrollEmpUuid(String payrollEmpUuid) { this.payrollEmpUuid = payrollEmpUuid; }

    public String getLeaveEmpUuid() { return leaveEmpUuid; }
    public void setLeaveEmpUuid(String leaveEmpUuid) { this.leaveEmpUuid = leaveEmpUuid; }

    public String getEmployeeServiceUuid() { return employeeServiceUuid; }
    public void setEmployeeServiceUuid(String employeeServiceUuid) { this.employeeServiceUuid = employeeServiceUuid; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    // ✅ NEW: Annual Structure ID getter/setter
    public Long getAnnualStructureId() { return annualStructureId; }
    public void setAnnualStructureId(Long annualStructureId) { this.annualStructureId = annualStructureId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}