package com.configserver.hrm.payrollService.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualSalaryStructureDTO {
    private Long id;
    private UUID employeeId;
    private String employeeName;
    private String designation;

    // Annual Components
    private double annualBasic;
    private double annualHra;
    private double annualAllowances;
    private double annualGross;
    private double annualNet;

    // Monthly Breakdown
    private double monthlyBasic;
    private double monthlyHra;
    private double monthlyAllowances;
    private double monthlyGross;
    private double monthlyNet;



    private String financialYear;
    private LocalDateTime createdAt;
    private String status;
    private String offerLetterId;
    private boolean addedToOfferLetter;


    public com.configserver.hrm.payrollService.entity.AnnualSalaryStructure toEntity() {
        return com.configserver.hrm.payrollService.entity.AnnualSalaryStructure.builder()
                .id(this.id)
                .employeeId(this.employeeId)
                .employeeName(this.employeeName)
                .designation(this.designation)
                .annualBasic(this.annualBasic)
                .annualHra(this.annualHra)
                .annualAllowances(this.annualAllowances)
                .annualGross(this.annualGross)
                .annualNet(this.annualNet)
                .monthlyBasic(this.monthlyBasic)
                .monthlyHra(this.monthlyHra)
                .monthlyAllowances(this.monthlyAllowances)
                .monthlyGross(this.monthlyGross)
                .monthlyNet(this.monthlyNet)
                .financialYear(this.financialYear)
                .createdAt(this.createdAt)
                .status(this.status)
                .offerLetterId(this.offerLetterId)
                .addedToOfferLetter(this.addedToOfferLetter)
                .build();
    }

}