package com.configserver.hrm.payrollService.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Employee Info ---
    private UUID employeeId;
    private String employeeName;
    private String designation;
    private String department;
    private String grade;
    private String vendorCode;
    private LocalDate dateOfJoining;

    private String pfNo;
    private String bankAccountNo;
    private String bankName;
    private String bankBranch;
    private String panNo;
    private String uanNo;

    // --- Attendance Details ---
    private int presentDays;
    private int holiday;
    private int paidLeave;
    private int gatePass;
    private int weekOff;
    private int totalSalaryDays;
    private int outDuty;
    private int unpaidLeave;
    private double totalWorkHours;

    // --- Salary / Earnings ---
    private double basicSalary;
    private double extraDaysWorked;
    private double houseRentAllowance;
    private double childrenEducation;
    private double monthlyOtherB;
    private double performanceBonus;
    private double specialAllowance;
    private double totalEarnings;

    // --- Deductions & Net ---
    private double totalDeductions;
    private double employeeNetPay;

    // --- Period & Status ---
    private LocalDate month;
    private int payslipMonth;   // NEW
    private int payslipYear;    // NEW

    private String status;

    // --- Calculation Mode ---
    private String calculationMode; // Hourly / Day-wise

    // --- Old compatibility fields ---
    private double basic;
    private double hra;
    private double allowances;
    private double grossSalary;
    private Double employeePf;
    private Double professionalTax;

    private double deductions;
    private double netSalary;
}
