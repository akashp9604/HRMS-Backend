package com.configserver.hrm.employeeService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String designation;
    private String department;
    private Double annualSalary;
    private String status;
    private LocalDate dateOfJoining;

    // New fields for profile
    private String profileImage;
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

    // OTP fields for forgot password
    private String otp;
    private LocalDateTime otpGeneratedTime;


}