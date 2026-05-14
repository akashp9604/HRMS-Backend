package com.configserver.hrm.employeeService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordDTO {

    @NotBlank(message = "Email is required")
    private String email;
}