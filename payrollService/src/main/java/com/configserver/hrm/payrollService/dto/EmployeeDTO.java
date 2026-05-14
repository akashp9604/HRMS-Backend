package com.configserver.hrm.payrollService.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {
    private UUID id;
    private String name;
    private String designation;
}