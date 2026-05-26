package com.configserver.hrm.employeeService.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePackageDTO {
    private Long employeeId;
    private String name;
    private String designation;
    private String role;
    private Double annualSalary;
    private LocalDate dateOfJoining;
}

