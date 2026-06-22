package com.configserver.hrm.mappingService.service;

import com.configserver.hrm.mappingService.config.RestTemplateConfig;
import com.configserver.hrm.mappingService.dto.EmployeeDTO;
import com.configserver.hrm.mappingService.dto.EmployeeIdMappingResponse;
import com.configserver.hrm.mappingService.entity.EmployeeIdMapping;
import com.configserver.hrm.mappingService.repository.EmployeeIdMappingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class EmployeeIdMappingService {

    @Autowired
    private EmployeeIdMappingRepository repository;

    @Autowired
    private RestTemplateConfig restTemplateConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmployeeIdMappingRepository employeeIdMappingRepository;

    // ✅ Helper method to extract JWT token from current request
    private String getJwtTokenFromContext() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7); // Remove "Bearer " prefix
            }
        }
        // If no token found in context, throw exception
        throw new RuntimeException("No JWT token found in request context. Please ensure you're passing Authorization header.");
    }

    // ========================
    // Create or update mapping
    // ========================
   /* public EmployeeIdMapping mapEmployeeIds(String attendanceId, String payrollUuid,
                                            String leaveUuid, String employeeServiceUuid,
                                            String employeeName, Long annualStructureId, String employeeEmail) {

        if (attendanceId != null) attendanceId = attendanceId.trim();
        if (payrollUuid != null) payrollUuid = payrollUuid.trim();
        if (leaveUuid != null) leaveUuid = leaveUuid.trim();
        if (employeeServiceUuid != null) employeeServiceUuid = employeeServiceUuid.trim();

        Optional<EmployeeIdMapping> existingOpt = repository.findByAttendanceEmpId(attendanceId);
        EmployeeIdMapping mapping;

        if (existingOpt.isPresent()) {
            mapping = existingOpt.get();
            if (payrollUuid != null) mapping.setPayrollEmpUuid(payrollUuid);
            if (leaveUuid != null) mapping.setLeaveEmpUuid(leaveUuid);
            if (employeeServiceUuid != null) mapping.setEmployeeServiceUuid(employeeServiceUuid);
            if (employeeName != null) mapping.setEmployeeName(employeeName);
            if (annualStructureId != null) mapping.setAnnualStructureId(annualStructureId);
            if (employeeEmail != null) mapping.setEmployeeEmail(employeeEmail);
        } else {
            mapping = new EmployeeIdMapping();
            if (attendanceId != null) mapping.setAttendanceEmpId(attendanceId);
            if (payrollUuid != null) mapping.setPayrollEmpUuid(payrollUuid);
            if (leaveUuid != null) mapping.setLeaveEmpUuid(leaveUuid);
            if (employeeServiceUuid != null) mapping.setEmployeeServiceUuid(employeeServiceUuid);
            if (employeeName != null) mapping.setEmployeeName(employeeName);
            if (annualStructureId != null) mapping.setAnnualStructureId(annualStructureId);
            if (employeeEmail != null) mapping.setEmployeeEmail(employeeEmail);
        }

        return repository.save(mapping);
    }*/
    // ✅ Map all IDs when employee created across services
    public EmployeeIdMapping mapEmployeeIds(
            String attendanceEmpId,
            String payrollEmpUuid,
            String leaveEmpUuid,
            String employeeServiceUuid,
            String employeeName,
            String employeeEmail,
            Long annualStructureId
    ) {

        Optional<EmployeeIdMapping> existing = repository.findByAttendanceEmpId(attendanceEmpId);
        EmployeeIdMapping mapping = existing.orElse(new EmployeeIdMapping());

        mapping.setAttendanceEmpId(attendanceEmpId);
        mapping.setPayrollEmpUuid(payrollEmpUuid);
        mapping.setLeaveEmpUuid(leaveEmpUuid);
        mapping.setEmployeeServiceUuid(employeeServiceUuid);
        mapping.setEmployeeName(employeeName);
        mapping.setAnnualStructureId(annualStructureId);
        mapping.setUpdatedAt(LocalDateTime.now());

        // ✅ Auto-fetch email from Employee Service if null or empty
        if ((employeeEmail == null || employeeEmail.isEmpty()) && employeeServiceUuid != null) {
            try {
                String employeeApiUrl = "http://localhost:8081/api/users/" + employeeServiceUuid;
                ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(employeeApiUrl, EmployeeDTO.class);
                if (response.getBody() != null && response.getBody().getEmail() != null) {
                    employeeEmail = response.getBody().getEmail();
                }
            } catch (Exception ex) {
                System.out.println("⚠️ Could not fetch email from Employee Service for UUID " + employeeServiceUuid);
            }
        }

        mapping.setEmployeeEmail(employeeEmail);

        repository.save(mapping);
        return mapping;
    }


    // ========================
    // Get mapping by IDs
    // ========================
    public Optional<EmployeeIdMapping> getByAttendanceId(String attendanceId) {
        return repository.findByAttendanceEmpId(attendanceId);
    }

    public Optional<EmployeeIdMapping> getByPayrollUuid(String payrollUuid) {
        return repository.findByPayrollEmpUuid(payrollUuid);
    }

    public Optional<EmployeeIdMapping> getByLeaveUuid(String leaveUuid) {
        System.out.println("🔍 Searching mapping for leaveEmpUuid = " + leaveUuid);
        Optional<EmployeeIdMapping> mapping = repository.findByLeaveEmpUuid(leaveUuid);
        mapping.ifPresentOrElse(
                m -> System.out.println("✅ Found mapping: " + m.getEmployeeName() + " - " + m.getEmployeeEmail()),
                () -> System.out.println("❌ No mapping found for: " + leaveUuid)
        );
        return mapping;
    }


    public Optional<EmployeeIdMapping> getByEmployeeUuid(String employeeUuid) {
        return repository.findByEmployeeServiceUuid(employeeUuid);
    }

    public Optional<EmployeeIdMapping> getByAnnualStructureId(Long annualStructureId) {
        return repository.findByAnnualStructureId(annualStructureId);
    }

    // ========================
    // Get all mappings
    // ========================
    public List<EmployeeIdMapping> getAllMappings() {
        return repository.findAll();
    }

    public List<EmployeeIdMapping> getMappingsWithoutAnnualStructure() {
        return repository.findMappingsWithoutAnnualStructure();
    }

    public List<EmployeeIdMapping> getMappingsWithAnnualStructure() {
        return repository.findMappingsWithAnnualStructure();
    }

    public List<EmployeeIdMapping> searchByEmployeeName(String name) {
        return repository.findByEmployeeNameContainingIgnoreCase(name);
    }

    // ========================
    // Update Annual Structure
    // ========================
    public EmployeeIdMapping updateAnnualStructureId(String employeeUuid, Long annualStructureId) {
        Optional<EmployeeIdMapping> mappingOpt = repository.findByEmployeeServiceUuid(employeeUuid);
        if (mappingOpt.isPresent()) {
            EmployeeIdMapping mapping = mappingOpt.get();
            mapping.setAnnualStructureId(annualStructureId);
            return repository.save(mapping);
        }
        throw new RuntimeException("Employee mapping not found for UUID: " + employeeUuid);
    }

    // ========================
    // Sync methods (existing)
    // ========================
   /* public List<EmployeeIdMapping> syncAllEmployeesFromEmployeeService() {
        String url = "http://localhost:8088/api/employees";
        String username = "ruchissonawane30@gmail.com";
        String password = "Admin@123";
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<EmployeeDTO[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                EmployeeDTO[].class
        );

        EmployeeDTO[] employees = response.getBody();
        List<EmployeeIdMapping> syncedEmployees = new ArrayList<>();

        if (employees != null) {
            for (EmployeeDTO emp : employees) {
                if (emp.getId() == null) continue;

                // ✅ Convert UUID to String safely
                String empId = emp.getId().toString();

                Optional<EmployeeIdMapping> existingOpt = repository.findByEmployeeServiceUuid(empId);
                EmployeeIdMapping mapping;

                if (existingOpt.isPresent()) {
                    mapping = existingOpt.get();
                    mapping.setEmployeeName(emp.getName());
                    mapping.setEmployeeEmail(emp.getEmail());
                } else {
                    mapping = new EmployeeIdMapping();
                    mapping.setEmployeeServiceUuid(empId);
                    mapping.setEmployeeName(emp.getName() != null ? emp.getName() : "Unknown");
                    mapping.setEmployeeEmail(emp.getEmail());
                    mapping.setLeaveEmpUuid(empId);
                    mapping.setPayrollEmpUuid(empId);
                    mapping.setAttendanceEmpId(null);
                    mapping.setAnnualStructureId(null);
                }

                repository.save(mapping);
                syncedEmployees.add(mapping);
            }
        }

        return syncedEmployees;
    }*/
    public List<EmployeeIdMapping> syncAllEmployeesFromEmployeeService() {
        try {
            String url = "http://localhost:8081/api/users";
            String jwtToken = getJwtTokenFromContext();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // ✅ Get response as String first
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // ✅ Parse and extract employees
            List<EmployeeDTO> employees = extractEmployees(rawResponse.getBody());

            // ✅ Process using streams
            return employees.stream()
                    .filter(emp -> emp.getId() != null)
                    .map(this::syncEmployee)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<EmployeeDTO> extractEmployees(String responseBody) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseBody);

        // ✅ Find array node
        JsonNode employeesNode = findEmployeeArray(root);

        if (employeesNode == null || employeesNode.isMissingNode()) {
            return new ArrayList<>();
        }

        EmployeeDTO[] employees = mapper.treeToValue(employeesNode, EmployeeDTO[].class);
        return Arrays.asList(employees);
    }

    private JsonNode findEmployeeArray(JsonNode root) {
        // ✅ If root itself is an array
        if (root.isArray()) {
            return root;
        }

        // ✅ Check common wrapper fields
        String[] fieldNames = {"content", "data", "employees", "results", "list"};

        for (String fieldName : fieldNames) {
            if (root.has(fieldName)) {
                JsonNode node = root.get(fieldName);
                if (node.isArray()) {
                    return node;
                }
            }
        }

        // ✅ If not found in common fields, iterate through all fields
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isArray()) {
                return field.getValue();
            }
        }

        return null;
    }

    private EmployeeIdMapping syncEmployee(EmployeeDTO emp) {
        String empIdStr = emp.getId().toString();

        EmployeeIdMapping mapping = repository
                .findByEmployeeServiceUuid(empIdStr)
                .map(existing -> {
                    existing.setEmployeeEmail(emp.getEmail());
                    existing.setEmployeeName(emp.getName());
                    return existing;
                })
                .orElseGet(() -> {
                    EmployeeIdMapping newMapping = new EmployeeIdMapping();
                    newMapping.setEmployeeServiceUuid(empIdStr);
                    newMapping.setEmployeeName(emp.getName() != null ? emp.getName() : "Unknown");
                    newMapping.setEmployeeEmail(emp.getEmail());
                    newMapping.setLeaveEmpUuid(empIdStr);
                    newMapping.setPayrollEmpUuid(empIdStr);
                    newMapping.setAttendanceEmpId(null);
                    newMapping.setAnnualStructureId(null);
                    return newMapping;
                });

        return repository.save(mapping);
    }


    public List<EmployeeIdMapping> syncAttendanceWithMapping() {
        String attendanceUrl = "http://localhost:8094/api/attendance/employees-info";
        ResponseEntity<List> response = restTemplate.getForEntity(attendanceUrl, List.class);
        List<Map<String, Object>> attendanceData = response.getBody();

        if (attendanceData == null) return repository.findAll();

        for (Map<String, Object> att : attendanceData) {
            String empId = (String) att.get("employeeId");
            String name = (String) att.get("name");

            Optional<EmployeeIdMapping> existing = repository.findByEmployeeNameIgnoreCase(name);
            existing.ifPresent(mapping -> {
                mapping.setAttendanceEmpId(empId);
                repository.save(mapping);
            });
        }

        return repository.findAll();
    }

    public List<EmployeeIdMapping> syncAllEmployeesAndAttendance() {
        syncAllEmployeesFromEmployeeService();
        return syncAttendanceWithMapping();
    }

    // ========================
    // Convert to Response DTO
    // ========================
    public EmployeeIdMappingResponse convertToResponse(EmployeeIdMapping mapping) {
        EmployeeIdMappingResponse response = new EmployeeIdMappingResponse();
        response.setId(mapping.getId());
        response.setAttendanceEmpId(mapping.getAttendanceEmpId());
        response.setPayrollEmpUuid(mapping.getPayrollEmpUuid());
        response.setLeaveEmpUuid(mapping.getLeaveEmpUuid());
        response.setEmployeeServiceUuid(mapping.getEmployeeServiceUuid());
        response.setEmployeeName(mapping.getEmployeeName());
        response.setEmployeeEmail(mapping.getEmployeeEmail());
        response.setAnnualStructureId(mapping.getAnnualStructureId());
        response.setCreatedAt(mapping.getCreatedAt());
        response.setUpdatedAt(mapping.getUpdatedAt());
        return response;
    }

    // ========================
    // ✅ NEW METHOD — Get Employee Email by Leave UUID
    // ========================
    public String getEmployeeEmailByLeaveUuid(String leaveUuid) {
        return repository.findEmployeeEmailByLeaveUuid(leaveUuid);
    }


    // ========================
    // DTO class for Employee Service
    // ========================

}
