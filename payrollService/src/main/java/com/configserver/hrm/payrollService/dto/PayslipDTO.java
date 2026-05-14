package com.configserver.hrm.payrollService.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayslipDTO {

    // 🔹 Employee Information
    private UUID employeeId;
    private String employeeName;
    private String department;
    private String designation;
    private String grade;
    private String vendorCode;
    private LocalDate dateOfJoining;
    private String pfNo;
    private String uanNo;
    private String panNo;
    private String bankName;
    private String bankBranch;
    private String bankAccountNo;

    // 🔹 Attendance Information
    private Integer presentDays;
    private Integer holiday;
    private Integer paidLeave;
    private Integer unpaidLeave;
    private Integer gatePass;
    private Integer weekOff;
    private Integer totalSalaryDays;
    private Integer outDuty;
    private Double totalWorkHours;

    private String calculationMode;

    // 🔹 Earnings Components
    private Double basicSalary;
    private Double houseRentAllowance;
    private Double specialAllowance;
    private Double extraDaysWorked;
    private Double childrenEducation;
    private Double monthlyOtherB;
    private Double performanceBonus;

    // 🔹 Summary
    private Double totalEarnings;

    private Double professionalTax;   // FIXED
    private Double pfAmount;          // FIXED

    private Double totalDeductions;
    private Double employeeNetPay;

    // 🔹 Period
    private LocalDate month;
    private Integer payslipMonth;
    private Integer payslipYear;

    private String status;

    // 🔹 Backward compatibility fields
    private Double basic;
    private Double hra;
    private Double allowances;
    private Double grossSalary;
    private Double deductions;
    private Double netSalary;
}
