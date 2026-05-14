package com.configserver.hrm.payrollService.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OfferLetterDTO {

    private String employeeId;
    private String name;
    private String designation;
    private String address;
    private LocalDate dateOfJoining;
    private LocalDate issueDate;
    private String referenceNumber;

    // Salary details - Updated to match PDF template structure
    private double annualBasic;
    private double annualHra;
    private double annualEducationAllowance;
    private double annualPersonalAllowance;
    private double annualSpecialAllowance;
    private double annualPf;
    private double annualGross;
    private double annualNet;

    private double monthlyBasic;
    private double monthlyHra;
    private double monthlyEducationAllowance;
    private double monthlyPersonalAllowance;
    private double monthlySpecialAllowance;
    private double monthlyPf;
    private double monthlyGross;
    private double monthlyNet;

    private double totalCTC;
}