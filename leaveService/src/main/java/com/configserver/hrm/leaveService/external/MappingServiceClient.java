package com.configserver.hrm.leaveService.external;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class MappingServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    private static final String BASE_URL = "http://localhost:8090/api/mapping/employee/";

    public EmployeeIdMappingResponse getEmployeeMapping(String id) {
        try {
            String url = BASE_URL + id;
            ResponseEntity<EmployeeIdMappingResponse> response =
                    restTemplate.getForEntity(url, EmployeeIdMappingResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                System.out.println("✅ Found mapping for: " + id);
                return response.getBody();
            }
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("❌ Mapping not found for ID: " + id);
        } catch (Exception e) {
            System.out.println("⚠️ Error fetching mapping for " + id + ": " + e.getMessage());
        }
        return null;
    }

    public String getEmployeeName(String employeeId) {
        EmployeeIdMappingResponse response = getEmployeeMapping(employeeId);
        return (response != null && response.getEmployeeName() != null)
                ? response.getEmployeeName()
                : "Unknown";
    }

    public String getLeaveEmpUuid(String employeeId) {
        EmployeeIdMappingResponse response = getEmployeeMapping(employeeId);
        return (response != null && response.getLeaveEmpUuid() != null)
                ? response.getLeaveEmpUuid()
                : employeeId;
    }
}
