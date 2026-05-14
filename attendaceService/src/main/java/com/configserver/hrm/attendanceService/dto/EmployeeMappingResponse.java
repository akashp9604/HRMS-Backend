package com.configserver.hrm.attendanceService.dto;

public class EmployeeMappingResponse {
    private String attendanceEmpId;
    private String payrollEmpUuid;
    private String leaveEmpUuid;
    private String employeeServiceUuid;
    private String employeeName;

    // ✅ Getters & Setters
    public String getAttendanceEmpId() {
        return attendanceEmpId;
    }
    public void setAttendanceEmpId(String attendanceEmpId) {
        this.attendanceEmpId = attendanceEmpId;
    }

    public String getPayrollEmpUuid() {
        return payrollEmpUuid;
    }
    public void setPayrollEmpUuid(String payrollEmpUuid) {
        this.payrollEmpUuid = payrollEmpUuid;
    }

    public String getLeaveEmpUuid() {
        return leaveEmpUuid;
    }
    public void setLeaveEmpUuid(String leaveEmpUuid) {
        this.leaveEmpUuid = leaveEmpUuid;
    }

    public String getEmployeeServiceUuid() {
        return employeeServiceUuid;
    }
    public void setEmployeeServiceUuid(String employeeServiceUuid) {
        this.employeeServiceUuid = employeeServiceUuid;
    }

    public String getEmployeeName() {
        return employeeName;
    }
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
}
