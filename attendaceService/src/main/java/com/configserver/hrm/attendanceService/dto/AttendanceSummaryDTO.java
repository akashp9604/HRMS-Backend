package com.configserver.hrm.attendanceService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceSummaryDTO
{
    private String employeeId;
    private int month;
    private int year;
    private int actualWorkingDays;
    private int presentDays;
    private int halfDays;
    private int absentDays;
    private int approvedLeaves;

    private List<String> leaveDates; // leave dates
    private int holidays;
    private int pendingPunches;
    private double actualWorkingHours; // new field

}
