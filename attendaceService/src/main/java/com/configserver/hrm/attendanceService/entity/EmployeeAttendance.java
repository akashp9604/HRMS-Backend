package com.configserver.hrm.attendanceService.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "attendance",
        indexes = {
                @Index(name = "idx_employee_date", columnList = "employeeId,date")
        }
)
public class EmployeeAttendance {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "employee_id")
    private String employeeId;
    private String employeeName;
    private LocalDate date;

    private String shift;

    private LocalTime inTime;
    private LocalTime outTime;

    private String lateIn;    // Excel column
    private String erlOut;    // Early out
    private Double workHours; // Work + OT hours
    private String overTime;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status; // Present, Absent, Leave etc.

    private String remark; // Extra info from Excel

    @Column(nullable = false)
    private boolean punchOutEmailSent = false;

    // Getter & Setter
    public boolean isPunchOutEmailSent() {
        return punchOutEmailSent;
    }

    public void setPunchOutEmailSent(boolean punchOutEmailSent) {
        this.punchOutEmailSent = punchOutEmailSent;
    }


    public EmployeeAttendance() {}

    public EmployeeAttendance(String employeeId, String employeeName, LocalDate date, String shift,
                              LocalTime inTime, LocalTime outTime, String lateIn, String erlOut,
                              Double workHours, String overTime, AttendanceStatus status, String remark) {
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

    // Getters & Setters
    public String getId() { return id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public LocalTime getInTime() { return inTime; }
    public void setInTime(LocalTime inTime) { this.inTime = inTime; }

    public LocalTime getOutTime() { return outTime; }
    public void setOutTime(LocalTime outTime) { this.outTime = outTime; }

    public String getLateIn() { return lateIn; }
    public void setLateIn(String lateIn) { this.lateIn = lateIn; }

    public String getErlOut() { return erlOut; }
    public void setErlOut(String erlOut) { this.erlOut = erlOut; }

    public Double getWorkHours() { return workHours; }
    public void setWorkHours(Double workHours) { this.workHours = workHours; }

    public String getOverTime() { return overTime; }
    public void setOverTime(String overTime) { this.overTime = overTime; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Column(name = "source_type")
    private String sourceType; // DAILY or MONTHLY

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

}
