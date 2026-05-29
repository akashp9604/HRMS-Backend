package com.configserver.hrm.employeeService.dto;

import com.configserver.hrm.employeeService.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResponse {
    private UUID id;
    private String name;
    private String email;
    private Role role;
}