package com.configserver.hrm.employeeService.controller;

import com.configserver.hrm.employeeService.dto.*;
import com.configserver.hrm.employeeService.entity.Employee;
import com.configserver.hrm.employeeService.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:3000")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // Existing register API
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody EmployeeDTO dto) {
        employeeService.registerEmployee(dto);
        return ResponseEntity.ok("Employee registered and password sent to email!");
    }
    // login
   /* @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginRequest request) {
        Employee employee = employeeService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok("Login successful! Welcome " + employee.getName() + " (" + employee.getRole() + ")");
    }*/

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        LoginResponse response = employeeService.login(email, password);
        return ResponseEntity.ok(response);
    }


    // Get all employees
    @GetMapping
    //@PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // Get employee by ID
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // Update employee
    @PutMapping("/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable UUID id, @RequestBody EmployeeDTO dto) {
        employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok("Employee updated successfully!");
    }

    // Delete employee
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable UUID id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deleted successfully!");
    }

    // ✅ New API: Fetch employees from Attendance and register
    @PostMapping("/import-from-attendance")
    public ResponseEntity<String> importEmployeesFromAttendance() {
        employeeService.registerEmployeesFromAttendance();
        return ResponseEntity.ok("Employees imported from Attendance successfully!");
    }

    @PostMapping("/create-manual")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Employee> createManualEmployee(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(employeeService.createManualEmployee(dto));
    }

    @GetMapping("/{id}/package")
    public ResponseEntity<EmployeePackageDTO> getEmployeePackage(@PathVariable UUID id) {
        EmployeePackageDTO dto = employeeService.getEmployeePackage(id);
        return ResponseEntity.ok(dto);
    }
    // Profile Management Endpoints
    @GetMapping("/profile")
    public ResponseEntity<Employee> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Employee profile = employeeService.getCurrentUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<Employee> updateProfile(@Valid @RequestBody ProfileUpdateDTO profileUpdateDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Employee currentUser = employeeService.getCurrentUserProfile(email);

        Employee updatedProfile = employeeService.updateProfile(currentUser.getId(), profileUpdateDTO);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfileImage(@RequestParam("image") MultipartFile image) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Employee currentUser = employeeService.getCurrentUserProfile(email);

        String imagePath = employeeService.updateProfileImage(currentUser.getId(), image);
        return ResponseEntity.ok("Profile image uploaded successfully: " + imagePath);
    }

    // Forgot Password Endpoints
    @PostMapping("/forgot-password")
    public ResponseEntity<String> initiateForgotPassword(@Valid @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        employeeService.initiateForgotPassword(forgotPasswordDTO);
        return ResponseEntity.ok("OTP sent to your email address");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO) {
        employeeService.resetPassword(passwordResetDTO);
        return ResponseEntity.ok("Password reset successfully");
    }

    // Serve profile images
    @GetMapping("/profile/image/{employeeId}")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable UUID employeeId) {
        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            if (employee.getProfileImage() == null) {
                return ResponseEntity.notFound().build();
            }

            Path imagePath = Paths.get(employee.getProfileImage());
            byte[] imageBytes = Files.readAllBytes(imagePath);

            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "image/jpeg"; // default
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ API to count total employees
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalEmployeeCount() {
        long count = employeeService.getTotalEmployeeCount();
        return ResponseEntity.ok(count);
    }

}