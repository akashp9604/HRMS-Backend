package com.configserver.hrm.payrollService.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class EmployeeClient {

    private final RestTemplate rest;
    private final String baseUrl;

    public EmployeeClient(RestTemplate rest, @Value("${employee.service.url}") String baseUrl) {
        this.rest = rest;
        this.baseUrl = baseUrl;
    }

    /**
     * Fetch basic package info for salary structure calculation.
     * GET {employee.service.url}/{employeeId}/package
     */
    public Map<String, Object> getEmployeePackage(UUID employeeId, String authHeader) {
        String url = baseUrl + "/" + employeeId + "/package";

        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = rest.exchange(url, HttpMethod.GET, request, Map.class);
        return response.getBody();
    }

    /**
     * ✅ Fetch complete employee details (for payslip generation)
     * GET {employee.service.url}/{employeeId}
     */
    public Map<String, Object> getEmployeeDetails(UUID employeeId, String authHeader) {
        String url = baseUrl + "/" + employeeId; // e.g. http://localhost:8088/api/employees/{id}

        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = rest.exchange(url, HttpMethod.GET, request, Map.class);
        return response.getBody();
    }
}
