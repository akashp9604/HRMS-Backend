package com.configserver.hrm.payrollService.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "annual_salary_structures")
public class AnnualSalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID employeeId;
    private String employeeName;
    private String designation;

    // Annual Components (CTC Breakdown)
    private double annualBasic;
    private double annualHra;
    private double annualAllowances;
    private double annualGross;
    private double annualNet;

    // Monthly Breakdown (for payslip reference)
    private double monthlyBasic;
    private double monthlyHra;
    private double monthlyAllowances;
    private double monthlyGross;
    private double monthlyNet;

    private String financialYear; // "2024-2025"
    private LocalDateTime createdAt;
    private String status; // DRAFT, APPROVED, ACTIVE

    // Offer Letter Fields
    private String offerLetterId;
    private LocalDateTime offerLetterDate;
    private boolean addedToOfferLetter;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = "DRAFT";
        addedToOfferLetter = false;
    }
}