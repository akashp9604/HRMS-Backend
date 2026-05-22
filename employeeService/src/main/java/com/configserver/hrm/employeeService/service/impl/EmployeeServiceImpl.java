package com.configserver.hrm.employeeService.service.impl;

import com.configserver.hrm.employeeService.client.AttendanceClient;
import com.configserver.hrm.employeeService.dto.*;
import com.configserver.hrm.employeeService.entity.Employee;
import com.configserver.hrm.employeeService.entity.Role;
import com.configserver.hrm.employeeService.exception.EmployeeException;
import com.configserver.hrm.employeeService.mail.EmailService;
import com.configserver.hrm.employeeService.repository.EmployeeRepository;
import com.configserver.hrm.employeeService.service.EmployeeService;
import com.configserver.hrm.employeeService.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AttendanceClient attendanceClient;

    @Value("${file.upload-dir:./uploads/profiles}")
    private String uploadDir;

    @Override
    public void registerEmployee(EmployeeDTO employeeDTO) {
        repository.findByEmail(employeeDTO.getEmail())
                .ifPresent(emp -> { throw new EmployeeException("Email already registered!"); });

        boolean isFirstAdmin = repository.count() == 0 && employeeDTO.getRole() == Role.ADMIN;
        if (!isFirstAdmin && employeeDTO.getRole() == Role.ADMIN) {
            throw new EmployeeException("Only an existing admin can register another admin!");
        }

        String rawPassword = PasswordGenerator.generatePassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);

        Employee employee = new Employee();
       // employee.setId(employeeDTO.getEmployeeId());
        employee.setName(employeeDTO.getName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setRole(employeeDTO.getRole());
        employee.setPassword(hashedPassword);
        employee.setDesignation(employeeDTO.getDesignation());
        employee.setDepartment(employeeDTO.getDepartment());
        employee.setAnnualSalary(employeeDTO.getAnnualSalary());
        employee.setStatus(employeeDTO.getStatus());
        employee.setDateOfJoining(employeeDTO.getDateOfJoining());

        repository.save(employee);
        emailService.sendPasswordEmail(employeeDTO.getEmail(), rawPassword);
    }

    @Override
    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));
    }

    @Override
    public void updateEmployee(Long id, EmployeeDTO employeeDTO) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));

        employee.setName(employeeDTO.getName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setRole(employeeDTO.getRole());
        employee.setDesignation(employeeDTO.getDesignation());
        employee.setDepartment(employeeDTO.getDepartment());
        employee.setAnnualSalary(employeeDTO.getAnnualSalary());
        employee.setStatus(employeeDTO.getStatus());
        employee.setDateOfJoining(employeeDTO.getDateOfJoining());
        employee.setPhoneNumber(employeeDTO.getPhoneNumber());
        employee.setAddress(employeeDTO.getAddress());

        // ✅ Added new fields
        employee.setPanNumber(employeeDTO.getPanNumber());
        employee.setPfNumber(employeeDTO.getPfNumber());
        employee.setUanNumber(employeeDTO.getUanNumber());
        employee.setBankName(employeeDTO.getBankName());
        employee.setBankBranch(employeeDTO.getBankBranch());
        employee.setBankAccountNumber(employeeDTO.getBankAccountNumber());
        employee.setVendorCode(employeeDTO.getVendorCode());

        repository.save(employee);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));
        repository.delete(employee);
    }

    @Override
    public void registerEmployeesFromAttendance() {
        List<Map<String, Object>> attendanceEmployees = attendanceClient.fetchEmployeesFromAttendance();

        if (attendanceEmployees == null || attendanceEmployees.isEmpty()) {
            System.out.println("No employees fetched from attendance service");
            return;
        }

        for (Map<String, Object> empData : attendanceEmployees) {
            if (empData == null) continue;  // skip null entries

            String email = (String) empData.get("email");
            String name = (String) empData.get("name");
            String employeeIdStr = (String) empData.get("employeeId");
            Long employeeId = null;
            if (employeeIdStr != null && !employeeIdStr.isEmpty()) {
                try {
                    employeeId = Long.parseLong(employeeIdStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid Long: " + employeeIdStr);
                }
                if (email == null || email.isEmpty()) continue; // skip invalid emails

                if (repository.findByEmail(email).isEmpty()) {
                    EmployeeDTO dto = new EmployeeDTO();
                    dto.setName(name != null ? name : "Unknown");
                    dto.setEmail(email);
                    dto.setRole(Role.EMPLOYEE);
                    dto.setEmployeeId(employeeId);
                    registerEmployee(dto);
                }
            }
        }
    }

    @Override
    public LoginResponse login(String email, String password) {
        Employee employee = repository.findByEmail(email)
                .orElseThrow(() -> new EmployeeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new EmployeeException("Invalid email or password");
        }

        return new LoginResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole()
        );
    }

    @Override
    public Employee createManualEmployee(EmployeeDTO dto) {
        // Check if email already exists
        repository.findByEmail(dto.getEmail())
                .ifPresent(emp -> { throw new EmployeeException("Email already registered!"); });

        // ✅ Restrict only one Admin
        if (dto.getRole() == Role.ADMIN) {
            long adminCount = repository.findAll().stream()
                    .filter(emp -> emp.getRole() == Role.ADMIN)
                    .count();

            if (adminCount >= 1) {
                throw new EmployeeException("Only one ADMIN allowed in the system!");
            }
        }

        // Generate password
        String rawPassword = PasswordGenerator.generatePassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // Create employee
        Employee employee = new Employee();
        employee.setName(dto.getName());
        employee.setEmail(dto.getEmail());
        employee.setRole(dto.getRole());
        employee.setDesignation(dto.getDesignation());
        employee.setDepartment(dto.getDepartment());
        employee.setAnnualSalary(dto.getAnnualSalary());
        employee.setStatus(dto.getStatus() != null ? dto.getStatus() : "Active");
        employee.setPassword(hashedPassword);
        employee.setDateOfJoining(dto.getDateOfJoining());

        // ✅ Add new fields
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setAddress(dto.getAddress());
        employee.setPanNumber(dto.getPanNumber());
        employee.setPfNumber(dto.getPfNumber());
        employee.setUanNumber(dto.getUanNumber());
        employee.setBankName(dto.getBankName());
        employee.setBankBranch(dto.getBankBranch());
        employee.setBankAccountNumber(dto.getBankAccountNumber());
        employee.setVendorCode(dto.getVendorCode());

        // Save employee
        repository.save(employee);

        // Send credentials
        emailService.sendPasswordEmail(dto.getEmail(), rawPassword);

        return employee;
    }

    @Override
    public EmployeePackageDTO getEmployeePackage(Long id) {
        Employee emp = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id " + id));

        return new EmployeePackageDTO(
                emp.getId(),
                emp.getName(),
                emp.getDesignation(),
                emp.getRole().name(),
                emp.getAnnualSalary(),
                emp.getDateOfJoining()
        );
    }
    @Override
    public Employee updateProfile(Long id, ProfileUpdateDTO profileUpdateDTO) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));

        employee.setName(profileUpdateDTO.getName());
        employee.setPhoneNumber(profileUpdateDTO.getPhoneNumber());
        employee.setAddress(profileUpdateDTO.getAddress());
        employee.setDesignation(profileUpdateDTO.getDesignation());
        employee.setDepartment(profileUpdateDTO.getDepartment());

        return repository.save(employee);
    }
    @Override
    public String updateProfileImage(Long id, MultipartFile profileImage) {
        if (profileImage.isEmpty()) {
            throw new EmployeeException("Profile image cannot be empty");
        }

        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeException("Employee not found with id: " + id));

        try {
            // Delete old image if exists
            if (employee.getProfileImage() != null) {
                Path oldImagePath = Paths.get(employee.getProfileImage());
                Files.deleteIfExists(oldImagePath);
            }

            // Save new image
            String imagePath = saveProfileImage(profileImage, id);
            employee.setProfileImage(imagePath);
            repository.save(employee);

            return imagePath;
        } catch (IOException e) {
            throw new EmployeeException("Failed to update profile image: " + e.getMessage());
        }
    }

    private String saveProfileImage(MultipartFile file, Long employeeId) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = employeeId + "_" + System.currentTimeMillis() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            // Save file
            Files.copy(file.getInputStream(), filePath);

            return filePath.toString();
        } catch (IOException e) {
            throw new EmployeeException("Failed to save profile image: " + e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return ".jpg";
        int lastIndex = fileName.lastIndexOf(".");
        return lastIndex == -1 ? ".jpg" : fileName.substring(lastIndex);
    }

    @Override
    public void initiateForgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        Employee employee = repository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new EmployeeException("Employee not found with email: " + forgotPasswordDTO.getEmail()));

        // Generate 6-digit OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Set OTP and expiry (10 minutes from now)
        employee.setOtp(otp);
        employee.setOtpGeneratedTime(LocalDateTime.now());
        repository.save(employee);

        // Send OTP via email
        emailService.sendOTPEmail(employee.getEmail(), otp);
    }

    @Override
    public void resetPassword(PasswordResetDTO passwordResetDTO) {
        Employee employee = repository.findByEmail(passwordResetDTO.getEmail())
                .orElseThrow(() -> new EmployeeException("Employee not found with email: " + passwordResetDTO.getEmail()));

        // Validate OTP
        if (employee.getOtp() == null || !employee.getOtp().equals(passwordResetDTO.getOtp())) {
            throw new EmployeeException("Invalid OTP");
        }

        // Check if OTP is expired (10 minutes)
        if (employee.getOtpGeneratedTime().isBefore(LocalDateTime.now().minusMinutes(10))) {
            throw new EmployeeException("OTP has expired");
        }

        // Update password
        String hashedPassword = passwordEncoder.encode(passwordResetDTO.getNewPassword());
        employee.setPassword(hashedPassword);

        // Clear OTP fields
        employee.setOtp(null);
        employee.setOtpGeneratedTime(null);

        repository.save(employee);

        // Send confirmation email
        emailService.sendPasswordResetConfirmation(employee.getEmail());
    }

    @Override
    public Employee getCurrentUserProfile(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new EmployeeException("Employee not found with email: " + email));
    }

    @Override
    public long getTotalEmployeeCount() {
        return repository.count();
    }

}
