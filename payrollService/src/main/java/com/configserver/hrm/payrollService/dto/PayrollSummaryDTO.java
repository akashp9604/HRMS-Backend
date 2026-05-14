package com.configserver.hrm.payrollService.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollSummaryDTO {
    private double totalPayroll;
    private double averageSalary;
    private long processedPayslips;
    private long pendingPayslips;
}
