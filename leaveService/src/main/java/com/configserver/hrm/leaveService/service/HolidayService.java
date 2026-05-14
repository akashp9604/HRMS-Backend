package com.configserver.hrm.leaveService.service;

import com.configserver.hrm.leaveService.dto.HolidayDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {
    private static final List<HolidayDTO> HOLIDAYS = List.of(
            new HolidayDTO("New Year's Day", "IND-PUNESHVJAINAGAR", LocalDate.of(2025, 1, 1)),
            new HolidayDTO("Ramzan Id/Eid-ul-Fitar", "IND-PUNESHVJAINAGAR", LocalDate.of(2025, 3, 31)),
            new HolidayDTO("Maharashtra Day", "IND-PUNESHVJAINAGAR", LocalDate.of(2025, 5, 1)),
            new HolidayDTO("Independence Day / Parsi New Year", "IND-PUNESHVJAINAGAR", LocalDate.of(2025, 8, 15)),
            new HolidayDTO("Ganesh Chaturthi", "IND-PUNESHVJAINAGAR", LocalDate.of(2025, 8, 27)),
            new HolidayDTO("Gandhi Jayanti / Vijaya Dashmi", "IND-PUNESHVJAINAGAR", LocalDate.of(2025, 10, 2)),
            new HolidayDTO("Diwali", "IND-PUNESHVJAINAGAR", LocalDate.of(2025, 10, 21))
    );

    public List<HolidayDTO> getHolidays() {
        return HOLIDAYS;
    }
}

