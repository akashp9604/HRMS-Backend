package com.configserver.hrm.payrollService.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class MappingServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    // ✅ Correct base URL for Mapping Service
    private final String BASE_URL = "http://localhost:8090/api/mapping";

    // ✅ Get mapping by payroll UUID
    public Map<String, Object> getByPayrollUuid(String payrollServiceUuid) {
        try {
            String url = BASE_URL + "/payroll/" + payrollServiceUuid;
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            return null; // return null if not found or failed
        }
    }

    // ✅ Create new mapping entry if missing
    public Map<String, Object> createMapping(String employeeName, String employeeServiceUuid,
                                             String payrollServiceUuid, String attendanceEmpId,
                                             String leaveServiceUuid) {
        String url = BASE_URL + "/map"; // ✅ correct endpoint (not /create)

        Map<String, Object> request = new HashMap<>();
        request.put("employeeName", employeeName);
        request.put("employeeServiceUuid", employeeServiceUuid);
        request.put("payrollServiceUuid", payrollServiceUuid);
        request.put("attendanceEmpId", attendanceEmpId);
        request.put("leaveServiceUuid", leaveServiceUuid);

        return restTemplate.postForObject(url, request, Map.class);
    }
}
