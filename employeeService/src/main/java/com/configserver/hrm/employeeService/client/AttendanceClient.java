package com.configserver.hrm.employeeService.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class AttendanceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // Use the new endpoint
    private static final String ATTENDANCE_API_URL = "http://localhost:8085/api/attendance/employees-info";

    public List<Map<String, Object>> fetchEmployeesFromAttendance() {
        return restTemplate.getForObject(ATTENDANCE_API_URL, List.class);
    }
}
