package com.configserver.hrm.leaveService.external;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private RestTemplate restTemplate;

    private final String baseUrl = "http://localhost:8085/api/attendance";

    public List<EmployeeAttendanceDTO> getTodayAttendance() {
        ResponseEntity<List<EmployeeAttendanceDTO>> response = restTemplate.exchange(
                baseUrl + "/import/daily",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<List<EmployeeAttendanceDTO>>() {}
        );
        return response.getBody();
    }

    public List<EmployeeAttendanceDTO> getAttendanceByDate(String date) {
        ResponseEntity<List<EmployeeAttendanceDTO>> response = restTemplate.exchange(
                baseUrl + "/import/by-date?date=" + date,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<List<EmployeeAttendanceDTO>>() {}
        );
        return response.getBody();
    }
}
