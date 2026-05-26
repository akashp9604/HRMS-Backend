package com.configserver.hrm.employeeService.dto;

import com.configserver.hrm.employeeService.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class EmployeeDTO {

    private Long employeeId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    private String designation;
    private String department;
    private Double annualSalary;
    private String status;

    @NotNull(message = "Date of Joining is required")
    private LocalDate dateOfJoining;

    // New fields
    private String phoneNumber;
    private String address;

    // ✅ Added new fields
    private String panNumber;
    private String pfNumber;
    private String uanNumber;
    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;
    private String vendorCode;

    // For image upload (not persisted in database directly)
    private transient MultipartFile profileImage;
}