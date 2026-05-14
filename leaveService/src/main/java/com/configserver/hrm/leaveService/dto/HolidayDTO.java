package com.configserver.hrm.leaveService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HolidayDTO
{
    private String name;
    private String location;
    private LocalDate date;
}
