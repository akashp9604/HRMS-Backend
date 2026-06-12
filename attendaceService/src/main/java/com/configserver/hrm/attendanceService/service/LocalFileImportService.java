package com.configserver.hrm.attendanceService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

@Service
public class LocalFileImportService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AttendanceService attendanceService;

    private static final String IMPORT_API_URL = "http://localhost:8085/api/attendance/import/etime-monthly";

    /**
     * Method 1: Call the existing API internally using RestTemplate
     */
    public Map<String, Object> importViaRestTemplate(String filePath, String sourceType) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        // Create multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("sourceType", sourceType);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        // Call the existing API
        ResponseEntity<Map> response = restTemplate.exchange(
                IMPORT_API_URL,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("Import failed with status: " + response.getStatusCode());
        }
    }

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