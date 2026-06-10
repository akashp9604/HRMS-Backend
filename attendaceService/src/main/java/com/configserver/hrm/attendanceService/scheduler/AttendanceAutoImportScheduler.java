package com.configserver.hrm.attendanceService.scheduler;

import com.configserver.hrm.attendanceService.service.LocalFileImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@EnableScheduling
public class AttendanceAutoImportScheduler {

    @Autowired
    private LocalFileImportService localFileImportService;

    private static final String FILE_PATH = "F:/ConfigServerLlp/HRMS-Backend/attendaceService/src/monthperformance01062026185649.xls";
    private static final boolean ENABLED = true;

    // Run every day at 12:00 AM
    @Scheduled(cron = "0 0 12 * * *")
    public void importAt12AM() {
        if (!ENABLED) return;

        System.out.println("=== Scheduled import started at: " + LocalDateTime.now() + " ===");
        try {
            Map<String, Object> result = localFileImportService.importDirectly(FILE_PATH, "ETIME_MONTHLY");
            System.out.println("Scheduled import completed: " + result.get("recordsImported") + " records imported");
        } catch (Exception e) {
            System.err.println("Scheduled import failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Run on the 1st of every month at 10:00 AM
    @Scheduled(cron = "0 0 10 1 * *")
    public void importOnFirstDayOfMonth() {
        if (!ENABLED) return;

        System.out.println("=== Monthly scheduled import started at: " + LocalDateTime.now() + " ===");
        try {
            Map<String, Object> result = localFileImportService.importDirectly(FILE_PATH, "ETIME_MONTHLY");
            System.out.println("Monthly import completed: " + result.get("recordsImported") + " records imported");
        } catch (Exception e) {
            System.err.println("Monthly import failed: " + e.getMessage());
        }
    }
}