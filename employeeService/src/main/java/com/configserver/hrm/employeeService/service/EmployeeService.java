package com.configserver.hrm.employeeService.service;

import com.configserver.hrm.employeeService.dto.*;
import com.configserver.hrm.employeeService.entity.Employee;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmployeeService {
    void registerEmployee(EmployeeDTO employeeDTO);
    List<Employee> getAllEmployees();
    Employee getEmployeeById(Long id);
    void updateEmployee(Long id, EmployeeDTO employeeDTO);
    void deleteEmployee(Long id);
    void registerEmployeesFromAttendance();
    LoginResponse login(String email, String password);
    Employee createManualEmployee(EmployeeDTO employeeDTO);
    EmployeePackageDTO getEmployeePackage(Long id);

    // New methods
    Employee updateProfile(Long id, ProfileUpdateDTO profileUpdateDTO);
    String updateProfileImage(Long id, MultipartFile profileImage);
    void initiateForgotPassword(ForgotPasswordDTO forgotPasswordDTO);
    void resetPassword(PasswordResetDTO passwordResetDTO);
    Employee getCurrentUserProfile(String email);
    long getTotalEmployeeCount();

}