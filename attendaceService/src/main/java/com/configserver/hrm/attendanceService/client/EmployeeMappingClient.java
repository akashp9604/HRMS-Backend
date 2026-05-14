package com.configserver.hrm.attendanceService.client;

import com.configserver.hrm.attendanceService.dto.EmployeeMappingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EmployeeMappingClient {

    @Autowired
    private RestTemplate restTemplate;

    private static final String BASE_URL = "http://localhost:8090/api/mapping/employee/";

    // ✅ Existing method: fetch by employee service UUID
    public EmployeeMappingResponse getByEmployeeUuid(String uuid) {
        try {
            return restTemplate.getForObject(BASE_URL + uuid, EmployeeMappingResponse.class);
        } catch (Exception e) {
            System.out.println("⚠️ Mapping fetch failed for UUID: " + uuid + " - " + e.getMessage());
            return null;
        }
    }

    // ✅ New method: fetch mapping by any type of ID
    public EmployeeMappingResponse getByAnyId(String id) {
        EmployeeMappingResponse mapping = null;

        try {
            // 1️⃣ Try employee service UUID
            mapping = restTemplate.getForObject(BASE_URL + id, EmployeeMappingResponse.class);
            if (mapping != null) return mapping;
        } catch (Exception ignored) {}

        try {
            // 2️⃣ Try attendance ID
            mapping = restTemplate.getForObject("http://localhost:8090/api/mapping/attendance/" + id, EmployeeMappingResponse.class);
            if (mapping != null) return mapping;
        } catch (Exception ignored) {}

        try {
            // 3️⃣ Try payroll UUID
            mapping = restTemplate.getForObject("http://localhost:8090/api/mapping/payroll/" + id, EmployeeMappingResponse.class);
            if (mapping != null) return mapping;
        } catch (Exception ignored) {}

        try {
            // 4️⃣ Try leave UUID
            mapping = restTemplate.getForObject("http://localhost:8090/api/mapping/leave/" + id, EmployeeMappingResponse.class);
        } catch (Exception ignored) {}

        return mapping;
    }
}
