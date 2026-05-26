package com.configserver.hrm.employeeService.config;

import com.configserver.hrm.employeeService.entity.Employee;
import com.configserver.hrm.employeeService.entity.Role;
import com.configserver.hrm.employeeService.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(1) // Ensures this runs before other CommandLineRunners
public class AdminSeeder implements CommandLineRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${app.seeding.enabled:true}") // Default to true if property not set
    private boolean seedingEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!seedingEnabled) {
            System.out.println("ℹ️ Seeding is disabled. Skipping admin creation.");
            return;
        }
        
        // Check if admin already exists
        if (employeeRepository.count() == 0) {
            createAdminUser();
        } else {
            // Check if specific admin email exists
            if (employeeRepository.findByEmail("admin@gmail.com").isEmpty()) {
                createAdminUser();
            } else {
                System.out.println("✅ Admin user already exists with email: admin@gmail.com");
            }
        }
    }

    private void createAdminUser() {
        Employee admin = new Employee();
        admin.setName("System Administrator");
        admin.setEmail("admin@gmail.com");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setDesignation("System Administrator");
        admin.setDepartment("IT Administration");
        admin.setAnnualSalary(0.0);
        admin.setStatus("Active");
        admin.setDateOfJoining(LocalDate.now());
        admin.setPhoneNumber("+91 9876543210");
        admin.setAddress("Corporate Office, Mumbai, India");
        
        // Optional: Add additional fields
        admin.setPanNumber("ABCDE1234F");
        admin.setBankName("Reserve Bank of India");
        admin.setBankBranch("Main Branch");
        admin.setBankAccountNumber("ADMIN00123456789");

        employeeRepository.save(admin);
        
        System.out.println("========================================");
        System.out.println("✅ ADMIN USER CREATED SUCCESSFULLY!");
        System.out.println("📧 Email: admin@gmail.com");
        System.out.println("🔑 Password: Admin@123");
        System.out.println("👤 Role: ADMIN");
        System.out.println("========================================");
    }
}