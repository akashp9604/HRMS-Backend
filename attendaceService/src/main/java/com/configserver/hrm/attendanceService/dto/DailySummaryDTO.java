package com.configserver.hrm.attendanceService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailySummaryDTO {
    private String date;
    private long totalPresent;
    private long totalAbsent;
}
