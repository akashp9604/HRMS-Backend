package com.configserver.hrm.leaveService.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeAttendanceDTO {
    private String employeeId;
    private LocalDate date;
    private boolean present;
}
