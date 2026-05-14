package com.configserver.hrm.payrollService.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AttendanceClient {

    @Autowired
    private RestTemplate restTemplate;

    private final String BASE_URL = "http://localhost:8085/api/attendance";

    // ✅ Calls monthly summary API (matches your endpoint exactly)
    public Map<String, Object> getMonthlySummaryForEmployee(UUID employeeId, String month) {
        try {
            String url = BASE_URL + "/employee/" + employeeId + "/monthly-summary?month=" + month;
            System.out.println("📞 Calling Monthly Summary API: " + url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("✅ Monthly summary received successfully");
            return response;
        } catch (Exception e) {
            System.out.println("❌ Error fetching monthly summary: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getMonthlySummaryForEmployeeStringId(String employeeId, String month) {
        try {
            String url = BASE_URL + "/employee/" + employeeId + "/monthly-summary?month=" + month;
            System.out.println("📞 Calling Monthly Summary API: " + url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("✅ Monthly summary received successfully");
            return response;
        } catch (Exception e) {
            System.out.println("❌ Error fetching monthly summary: " + e.getMessage());
            return null;
        }
    }

    // ✅ Keep existing old methods for compatibility
    public Map<String, Object> getMonthlyAttendance(UUID employeeId, int month, int year) {
        String url = BASE_URL + "/" + employeeId.toString() + "/summary?month=" + month + "&year=" + year;
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> getMonthlyAttendanceStringId(String attendanceId, int month, int year) {
        String url = BASE_URL + "/" + attendanceId + "/summary?month=" + month + "&year=" + year;
        return restTemplate.getForObject(url, Map.class);
    }

    public String findAttendanceIdByName(String employeeName) {
        String url = BASE_URL + "/employees-info";
        List<Map<String, Object>> allAttendance = restTemplate.getForObject(url, List.class);

        if (allAttendance != null) {
            for (Map<String, Object> record : allAttendance) {
                String name = (String) record.get("name");
                if (employeeName.equalsIgnoreCase(name)) {
                    return (String) record.get("employeeId");
                }
            }
        }
        return null;
    }

    // ✅ Helper: Extract total work hours from summary
    public double getTotalWorkHours(Map<String, Object> summary) {
        if (summary != null && summary.containsKey("totalWorkHours")) {
            try {
                return Double.parseDouble(summary.get("totalWorkHours").toString());
            } catch (Exception ignored) {}
        }
        return 0.0;
    }

    // ✅ Helper: Extract actual working days dynamically (from summary OR calendar)
    public int getTotalWorkingDays(Map<String, Object> summary, String month) {
        if (summary != null && summary.containsKey("actualWorkingDays")) {
            try {
                return ((Number) summary.get("actualWorkingDays")).intValue();
            } catch (Exception ignored) {}
        }

        // fallback → calculate working days (Mon–Fri) dynamically
        try {
            YearMonth ym = YearMonth.parse(month);
            int count = 0;
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                DayOfWeek dow = ym.atDay(day).getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) count++;
            }
            return count;
        } catch (Exception e) {
            return 22; // safe fallback if parsing fails
        }
    }
}
