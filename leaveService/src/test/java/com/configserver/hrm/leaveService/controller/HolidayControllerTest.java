package com.configserver.hrm.leaveService.controller;

import com.configserver.hrm.leaveService.dto.HolidayDTO;
import com.configserver.hrm.leaveService.service.HolidayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class HolidayControllerTest {

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private HolidayController holidayController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(holidayController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getHolidaysBetween_ShouldReturnHolidaysInRange() throws Exception {
        // Given
        HolidayDTO holiday1 = new HolidayDTO();
        holiday1.setDate(LocalDate.of(2024, 6, 1));
        holiday1.setName("Children's Day");

        HolidayDTO holiday2 = new HolidayDTO();
        holiday2.setDate(LocalDate.of(2024, 8, 15));
        holiday2.setName("Independence Day");

        List<HolidayDTO> allHolidays = Arrays.asList(holiday1, holiday2);

        when(holidayService.getHolidays()).thenReturn(allHolidays);

        // For debugging - print the actual response
        String response = mockMvc.perform(get("/api/holidays/between")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        System.out.println("Actual response: " + response);

        // If date is serialized as array, expect something like: {"date":[2024,6,1],"name":"Children's Day"}
        // Then you'd need to adjust assertions accordingly:
        mockMvc.perform(get("/api/holidays/between")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date[0]").value(2024))  // year
                .andExpect(jsonPath("$[0].date[1]").value(6))     // month
                .andExpect(jsonPath("$[0].date[2]").value(1))     // day
                .andExpect(jsonPath("$[0].name").value("Children's Day"));
    }

    @Test
    void getHolidaysBetween_ShouldReturnEmptyList_WhenNoHolidaysInRange() throws Exception {
        // Given
        List<HolidayDTO> allHolidays = Arrays.asList(
                createHoliday(LocalDate.of(2024, 1, 1), "New Year"),
                createHoliday(LocalDate.of(2024, 12, 25), "Christmas")
        );

        when(holidayService.getHolidays()).thenReturn(allHolidays);

        // When & Then
        mockMvc.perform(get("/api/holidays/between")
                        .param("from", "2024-02-01")
                        .param("to", "2024-02-28")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getHolidaysBetween_ShouldFilterHolidaysExactlyAtBoundaries() throws Exception {
        // Given
        LocalDate boundaryDate = LocalDate.of(2024, 6, 15);
        HolidayDTO boundaryHoliday = createHoliday(boundaryDate, "Boundary Holiday");

        when(holidayService.getHolidays()).thenReturn(Collections.singletonList(boundaryHoliday));

        // When & Then - From date exactly matches holiday date
        mockMvc.perform(get("/api/holidays/between")
                        .param("from", "2024-06-15")
                        .param("to", "2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private HolidayDTO createHoliday(LocalDate date, String name) {
        HolidayDTO holiday = new HolidayDTO();
        holiday.setDate(date);
        holiday.setName(name);
        return holiday;
    }
}