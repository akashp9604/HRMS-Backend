package com.configserver.hrm.leaveService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class LeaveRequestDTO {

    @NotBlank(message = "Employee ID is required")
    private UUID employeeId;

    @NotBlank(message = "Leave type is required")
    private String leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private String reason;

    private Double hoursWorked;

    public Boolean getPartialDay() {
        return isPartialDay;
    }

    public void setPartialDay(Boolean partialDay) {
        isPartialDay = partialDay;
    }

    public Double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(Double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    private Boolean isPartialDay = false;

//    private String medicalCertificateBase64;   // Medical certificate file in base64 format
//
//    private String medicalCertificateName;

    public @NotBlank(message = "Employee ID is required") UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(@NotBlank(message = "Employee ID is required") UUID employeeId) {
        this.employeeId = employeeId;
    }

    public @NotBlank(message = "Leave type is required") String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(@NotBlank(message = "Leave type is required") String leaveType) {
        this.leaveType = leaveType;
    }

    public @NotNull(message = "Start date is required") LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(@NotNull(message = "Start date is required") LocalDate startDate) {
        this.startDate = startDate;
    }

    public @NotNull(message = "End date is required") LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(@NotNull(message = "End date is required") LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

//    public String getMedicalCertificateBase64() {
//        return medicalCertificateBase64;
//    }
//
//    public void setMedicalCertificateBase64(String medicalCertificateBase64) {
//        this.medicalCertificateBase64 = medicalCertificateBase64;
//    }
//
//    public String getMedicalCertificateName() {
//        return medicalCertificateName;
//    }
//
//    public void setMedicalCertificateName(String medicalCertificateName) {
//        this.medicalCertificateName = medicalCertificateName;
//    }
}
