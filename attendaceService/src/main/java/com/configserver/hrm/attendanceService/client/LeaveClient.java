package com.configserver.hrm.attendanceService.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LeaveClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "http://localhost:8087/api"; // leave-service base URL

    // 🔹 1. Get all approved leaves for single employee
    public List<Map<String, Object>> getApprovedLeaves(String employeeId) {
        String url = String.format("%s/leaves/employee/%s/approved", BASE_URL, employeeId);
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    // 🔹 2. Get all approved leaves for all employees
    public List<Map<String, Object>> getAllApprovedLeaves() {
        String url = String.format("%s/leaves/approved", BASE_URL);
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    // 🔹 3. Filter leaves by date range (helper method)
    public List<Map<String, Object>> filterLeavesByDate(List<Map<String, Object>> leaves, LocalDate from, LocalDate to) {
        return leaves.stream()
                .filter(l -> {
                    LocalDate start = LocalDate.parse((String) l.get("startDate"));
                    LocalDate end = LocalDate.parse((String) l.get("endDate"));
                    return !(end.isBefore(from) || start.isAfter(to)); // overlap check
                })
                .collect(Collectors.toList());
    }

    // 🔹 4. Get holidays between dates
    public List<Map<String, Object>> getHolidays(LocalDate from, LocalDate to) {
        String url = String.format("%s/holidays/between?from=%s&to=%s", BASE_URL, from, to);
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }
}
