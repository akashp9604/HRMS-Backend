package com.configserver.hrm.leaveService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_leave")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeLeave {

    @Id
    @GeneratedValue
    @Column(length = 36)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    private String employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveStatus status;

    private String approvedBy;
    private LocalDateTime approvedOn;
    private LocalDateTime appliedOn;

    // Fields for hourly tracking
    private Double hoursWorked;
    private Boolean isConvertedFromPaid = false;
    private String conversionReason;

    // NEW: Fields for WFH conversion tracking
    private Boolean convertedFromWFH = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveType convertedLeaveType;

    private Integer convertedPaidDays = 0;
    private Integer convertedUnpaidDays = 0;

//    //  FIELDS FOR MEDICAL CERTIFICATE
//    @Column(columnDefinition = "LONGTEXT")
//    private String medicalCertificateBase64;  // Store base64 string of medical certificate
//
//    @Column(name = "medical_certificate_name")
//    private String medicalCertificateName;

    @PrePersist
    protected void onCreate() {
        appliedOn = LocalDateTime.now();
        if (isConvertedFromPaid == null) {
            isConvertedFromPaid = false;
        }
        if (convertedFromWFH == null) {
            convertedFromWFH = false;
        }
        if (convertedPaidDays == null) {
            convertedPaidDays = 0;
        }
        if (convertedUnpaidDays == null) {
            convertedUnpaidDays = 0;
        }
    }
}