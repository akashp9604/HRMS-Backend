package com.configserver.hrm.attendanceService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

@Service
public class LocalFileImportService {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * Method 2: Directly call the service method (preferred - no HTTP overhead)
     */
    public Map<String, Object> importDirectly(String filePath, String sourceType) throws Exception {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        // Convert File to MultipartFile
        FileInputStream inputStream = new FileInputStream(file);
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                file.getName(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                inputStream
        );

        // Call the existing service method
        var importedData = attendanceService.importEtimeMonthlyReport(multipartFile, sourceType);

        inputStream.close();

        // Prepare response
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Monthly attendance imported successfully from local file");
        response.put("filePath", filePath);
        response.put("fileName", file.getName());
        response.put("recordsImported", importedData.size());
        response.put("employeesProcessed", importedData.stream().map(e -> e.getEmployeeId()).distinct().count());
        response.put("data", importedData);

        return response;
    }
}