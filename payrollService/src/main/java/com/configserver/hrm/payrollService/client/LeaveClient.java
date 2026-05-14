package com.configserver.hrm.payrollService.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class LeaveClient {

    @Autowired
    private RestTemplate restTemplate;

    private final String BASE_URL = "http://localhost:8087/api";

    public List<Map<String, Object>> getApprovedLeaves(UUID employeeId, LocalDate from, LocalDate to) {
        String url = String.format(BASE_URL + "/leaves/employee/%s/between?from=%s&to=%s",
                employeeId, from, to);
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    public List<Map<String, Object>> getHolidays(LocalDate from, LocalDate to) {
        String url = String.format(BASE_URL + "/holidays/between?from=%s&to=%s", from, to);
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }
}
