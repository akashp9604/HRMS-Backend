package com.configserver.hrm.attendanceService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public class AttendanceRequestDTO {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Employee Name is required")
    private String employeeName;

    @NotBlank(message = "Shift is required")
    private String shift;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid In Time format, expected HH:mm")
    private String inTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid Late In format, expected HH:mm")
    private String lateIn;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid Early Out format, expected HH:mm")
    private String erlOut;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid Out Time format, expected HH:mm")
    private String outTime;

    private String workOt;
    private String overTime;
    private String status;
    private String remark;

    @NotNull(message = "Attendance date is required")
    private LocalDate date;

    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public String getInTime() { return inTime; }
    public void setInTime(String inTime) { this.inTime = inTime; }

    public String getLateIn() { return lateIn; }
    public void setLateIn(String lateIn) { this.lateIn = lateIn; }

    public String getErlOut() { return erlOut; }
    public void setErlOut(String erlOut) { this.erlOut = erlOut; }

    public String getOutTime() { return outTime; }
    public void setOutTime(String outTime) { this.outTime = outTime; }

    public String getWorkOt() { return workOt; }
    public void setWorkOt(String workOt) { this.workOt = workOt; }

    public String getOverTime() { return overTime; }
    public void setOverTime(String overTime) { this.overTime = overTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
