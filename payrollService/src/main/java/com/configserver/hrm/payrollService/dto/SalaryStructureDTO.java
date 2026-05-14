package com.configserver.hrm.payrollService.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructureDTO {
    private UUID employeeId;
    private double basic;
    private double hra;
    private double allowances;
    private double deductions;
}
