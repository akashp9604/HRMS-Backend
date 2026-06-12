package com.configserver.hrm.payrollService.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructureDTO {
    private Long employeeId;
    private double basic;
    private double hra;
    private double allowances;
    private double deductions;
}
