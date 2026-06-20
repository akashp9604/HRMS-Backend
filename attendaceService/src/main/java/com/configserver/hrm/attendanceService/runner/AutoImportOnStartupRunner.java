package com.configserver.hrm.attendanceService.runner;

import com.configserver.hrm.attendanceService.service.LocalFileImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AutoImportOnStartupRunner implements CommandLineRunner {

    @Autowired
    private LocalFileImportService localFileImportService;

    private static final String FILE_PATH = "F:\\ConfigServerLlp\\HRMS-Backend\\attendaceService\\src\\monthperformance01062026185649.xls";
    private static final boolean IMPORT_ON_STARTUP = true; // Set to false to disable

    @Override
    public void run(String... args) throws Exception {
        if (IMPORT_ON_STARTUP) {
            System.out.println("=== Auto-importing attendance on application startup ===");
            try {
                Thread.sleep(5000); // Wait 5 seconds for app to fully start
                Map<String, Object> result = localFileImportService.importDirectly(FILE_PATH, "ETIME_MONTHLY");
                System.out.println("Startup import completed: " + result.get("recordsImported") + " records imported");
            } catch (Exception e) {
                System.err.println("Startup import failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}