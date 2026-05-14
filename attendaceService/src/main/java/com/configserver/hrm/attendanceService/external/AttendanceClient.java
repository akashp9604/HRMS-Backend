package com.configserver.hrm.attendanceService.external;

import com.configserver.hrm.attendanceService.dto.EmployeeAttendanceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AttendanceClient {

    @Autowired
    private RestTemplate restTemplate;

    private final String attendanceServiceBaseUrl = "http://localhost:8085/api/attendance";

    // ✅ Import today attendance
    public List<EmployeeAttendanceDTO> importTodayAttendance() {
        String url = attendanceServiceBaseUrl + "/import/daily";
        ResponseEntity<List<EmployeeAttendanceDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<List<EmployeeAttendanceDTO>>() {}
        );
        return response.getBody();
    }

    // ✅ Import attendance by date
    public List<EmployeeAttendanceDTO> importAttendanceByDate(String date) {
        String url = attendanceServiceBaseUrl + "/import/by-date?date=" + date;
        ResponseEntity<List<EmployeeAttendanceDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<List<EmployeeAttendanceDTO>>() {}
        );
        return response.getBody();
    }

    // ✅ Get daily attendance (GET)
    public List<EmployeeAttendanceDTO> getDailyAttendance(String date) {
        String url = attendanceServiceBaseUrl + "/daily?date=" + date;
        ResponseEntity<List<EmployeeAttendanceDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<EmployeeAttendanceDTO>>() {}
        );
        return response.getBody();
    }
}
