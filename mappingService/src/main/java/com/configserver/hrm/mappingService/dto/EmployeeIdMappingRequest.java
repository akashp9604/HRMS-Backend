package com.configserver.hrm.mappingService.dto;

public class EmployeeIdMappingRequest {

    private String attendanceEmpId;
    private String employeeServiceUuid;
    private String payrollServiceUuid;
    private String leaveServiceUuid;
    private String employeeName;

    // ✅ NEW FIELD: Annual Structure ID
    private Long annualStructureId;

    private String employeeEmail;

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }


    // getters and setters
    public String getAttendanceEmpId() { return attendanceEmpId; }
    public void setAttendanceEmpId(String attendanceEmpId) { this.attendanceEmpId = attendanceEmpId; }

    public String getEmployeeServiceUuid() { return employeeServiceUuid; }
    public void setEmployeeServiceUuid(String employeeServiceUuid) { this.employeeServiceUuid = employeeServiceUuid; }

    public String getPayrollServiceUuid() { return payrollServiceUuid; }
    public void setPayrollServiceUuid(String payrollServiceUuid) { this.payrollServiceUuid = payrollServiceUuid; }

    public String getLeaveServiceUuid() { return leaveServiceUuid; }
    public void setLeaveServiceUuid(String leaveServiceUuid) { this.leaveServiceUuid = leaveServiceUuid; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    // ✅ NEW: Annual Structure ID getter/setter
    public Long getAnnualStructureId() { return annualStructureId; }
    public void setAnnualStructureId(Long annualStructureId) { this.annualStructureId = annualStructureId; }
}