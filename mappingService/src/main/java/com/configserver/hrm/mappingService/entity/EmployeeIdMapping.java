package com.configserver.hrm.mappingService.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_id_mapping")
public class EmployeeIdMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendance_emp_id", unique = true)
    private String attendanceEmpId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "employee_service_uuid", unique = true)
    private String employeeServiceUuid;

    @Column(name = "leave_emp_uuid", unique = true)
    private String leaveEmpUuid;

    @Column(name = "payroll_emp_uuid", unique = true)
    private String payrollEmpUuid;

    @Column(name = "annual_structure_id")
    private Long annualStructureId;

    @Column(name = "employee_email")
    private String employeeEmail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAttendanceEmpId() { return attendanceEmpId; }
    public void setAttendanceEmpId(String attendanceEmpId) { this.attendanceEmpId = attendanceEmpId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeServiceUuid() { return employeeServiceUuid; }
    public void setEmployeeServiceUuid(String employeeServiceUuid) { this.employeeServiceUuid = employeeServiceUuid; }

    public String getLeaveEmpUuid() { return leaveEmpUuid; }
    public void setLeaveEmpUuid(String leaveEmpUuid) { this.leaveEmpUuid = leaveEmpUuid; }

    public String getPayrollEmpUuid() { return payrollEmpUuid; }
    public void setPayrollEmpUuid(String payrollEmpUuid) { this.payrollEmpUuid = payrollEmpUuid; }

    public Long getAnnualStructureId() { return annualStructureId; }
    public void setAnnualStructureId(Long annualStructureId) { this.annualStructureId = annualStructureId; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "EmployeeIdMapping{" +
                "id=" + id +
                ", name='" + employeeName + '\'' +
                ", email='" + employeeEmail + '\'' +
                ", leaveEmpUuid='" + leaveEmpUuid + '\'' +
                '}';
    }
}
