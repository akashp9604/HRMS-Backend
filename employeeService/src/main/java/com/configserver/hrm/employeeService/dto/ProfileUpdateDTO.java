package com.configserver.hrm.employeeService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String phoneNumber;
    private String address;
    private String designation;
    private String department;

    // Remove MultipartFile since we're using JSON
    // private MultipartFile profileImage;
}