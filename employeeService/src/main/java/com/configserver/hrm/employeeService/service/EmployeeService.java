package com.configserver.hrm.employeeService.service;

import com.configserver.hrm.employeeService.dto.*;
import com.configserver.hrm.employeeService.entity.Employee;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    void registerEmployee(EmployeeDTO employeeDTO);
    List<Employee> getAllEmployees();
    Employee getEmployeeById(UUID id);
    void updateEmployee(UUID id, EmployeeDTO employeeDTO);
    void deleteEmployee(UUID id);
    void registerEmployeesFromAttendance();
    LoginResponse login(String email, String password);
    Employee createManualEmployee(EmployeeDTO employeeDTO);
    EmployeePackageDTO getEmployeePackage(UUID id);

    // New methods
    Employee updateProfile(UUID id, ProfileUpdateDTO profileUpdateDTO);
    String updateProfileImage(UUID id, MultipartFile profileImage);
    void initiateForgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    void resetPassword(PasswordResetDTO passwordResetDTO);
    Employee getCurrentUserProfile(String email);
    long getTotalEmployeeCount();

}