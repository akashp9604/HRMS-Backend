package com.configserver.hrm.employeeService.dto;

import com.configserver.hrm.employeeService.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
}
