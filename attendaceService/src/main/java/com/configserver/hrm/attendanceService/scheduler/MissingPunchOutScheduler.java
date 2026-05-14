package com.configserver.hrm.attendanceService.scheduler;

import com.configserver.hrm.attendanceService.config.EmployeeEmailConfig;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import com.configserver.hrm.attendanceService.repository.EmployeeAttendanceRepository;
import com.configserver.hrm.attendanceService.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class MissingPunchOutScheduler {

    @Autowired
    private EmployeeAttendanceRepository repository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmployeeEmailConfig employeeEmailConfig;

    // Runs every 10 minutes
   /* @Scheduled(fixedRate = 600000) // 600,000 ms = 10 min
    public void sendMissingPunchOutReminders() {
        LocalDate today = LocalDate.now();
        List<EmployeeAttendance> records = repository.findByDate(today);

        LocalTime now = LocalTime.now();

        for (EmployeeAttendance record : records) {
            if (record.getInTime() != null && record.getOutTime() == null) {

                // Check if 8 hours have passed since punch-in
                if (record.getInTime().plusHours(8).isBefore(now)) {

                    String email = employeeEmailConfig.getEmailByEmployeeId(record.getEmployeeId());
                    if (email != null && !email.isEmpty()) {
                        emailService.sendPunchOutReminder(email, record.getEmployeeName());
                        System.out.println("✅ Missing punch-out reminder sent to: " + record.getEmployeeName());
                    } else {
                        System.out.println("⚠️ No email found for: " + record.getEmployeeName());
                    }
                }
            }
        }
    }*/
}
