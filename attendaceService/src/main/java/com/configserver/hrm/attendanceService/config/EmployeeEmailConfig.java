package com.configserver.hrm.attendanceService.config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmployeeEmailConfig {

    private final Map<String, String> employeeEmails = new HashMap<>();

    public EmployeeEmailConfig() {
        // Add employee email manually
        employeeEmails.put("20250304-O-L-53", "rutuja.kothawade@configserverllp.com");
        employeeEmails.put("20250304-O-L-51", "nikhil.shinde@configserverllp.com");
        employeeEmails.put("20250304-O-L-52", "ruchita.sonawane@configserverllp.com");
        employeeEmails.put("20250304-O-L-54", "vaishnavi.kachave@configserverllp.com");
        employeeEmails.put("20250304-O-L-55", "vivek.chopade@configserverllp.com");
        employeeEmails.put("20250304-O-L-57", "sagar.lahade@configserverllp.com");
        employeeEmails.put("20250304-O-L-58", "arti.jadhav@configserverllp.com");
        employeeEmails.put("20250304-O-L-59", "Srikant.Chintawar@configserverllp.com");
        employeeEmails.put("61", "ashwin.narkhede@configserverllp.com");
        employeeEmails.put("62", "ravishankar.shinde@configserverllp.com");
        employeeEmails.put("63", "saujanya.nagamwad@configserverllp.com");
        employeeEmails.put("64", "mayur.jadhav@configserverllp.com");
        employeeEmails.put("65", "akash.pawale@configserverllp.com");

        // Add more employees here
    }

    public String getEmailByEmployeeId(String employeeId) {
        return employeeEmails.get(employeeId);
    }
}
