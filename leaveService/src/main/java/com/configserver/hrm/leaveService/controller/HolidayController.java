package com.configserver.hrm.leaveService.controller;

import com.configserver.hrm.leaveService.dto.HolidayDTO;
import com.configserver.hrm.leaveService.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    @GetMapping("/between")
    public ResponseEntity<List<HolidayDTO>> getHolidaysBetween(
            @RequestParam String from,
            @RequestParam String to) {

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        List<HolidayDTO> holidays = holidayService.getHolidays().stream()
                .filter(h -> !h.getDate().isBefore(fromDate) && !h.getDate().isAfter(toDate))
                .collect(Collectors.toList());

        return ResponseEntity.ok(holidays);
    }
}

