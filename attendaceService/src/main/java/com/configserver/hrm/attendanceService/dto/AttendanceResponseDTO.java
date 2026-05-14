package com.configserver.hrm.attendanceService.dto;

import java.time.LocalDate;

public class AttendanceResponseDTO {

    private String id;             // DB generated ID
    private String employeeId;     // Empcode
    private String employeeName;   // Name
    private LocalDate date;        // Attendance Date
    private String shift;
    private String inTime;
    private String outTime;
    private String lateIn;
    private String erlOut;
    private Double workHours;      // calculated from Work+OT
    private String overTime;
    private String status;
    private String remark;

    public AttendanceResponseDTO() {
    }

    public AttendanceResponseDTO(String id, String employeeId, String employeeName, LocalDate date,
                                 String shift, String inTime, String outTime, String lateIn,
                                 String erlOut, Double workHours, String overTime,
                                 String status, String remark) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.shift = shift;
        this.inTime = inTime;
        this.outTime = outTime;
        this.lateIn = lateIn;
        this.erlOut = erlOut;
        this.workHours = workHours;
        this.overTime = overTime;
        this.status = status;
        this.remark = remark;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public String getInTime() { return inTime; }
    public void setInTime(String inTime) { this.inTime = inTime; }

    public String getOutTime() { return outTime; }
    public void setOutTime(String outTime) { this.outTime = outTime; }

    public String getLateIn() { return lateIn; }
    public void setLateIn(String lateIn) { this.lateIn = lateIn; }

    public String getErlOut() { return erlOut; }
    public void setErlOut(String erlOut) { this.erlOut = erlOut; }

    public Double getWorkHours() { return workHours; }
    public void setWorkHours(Double workHours) { this.workHours = workHours; }

    public String getOverTime() { return overTime; }
    public void setOverTime(String overTime) { this.overTime = overTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
