package com.configserver.hrm.mappingService.controller;

import com.configserver.hrm.mappingService.dto.EmployeeIdMappingRequest;
import com.configserver.hrm.mappingService.dto.EmployeeIdMappingResponse;
import com.configserver.hrm.mappingService.entity.EmployeeIdMapping;
import com.configserver.hrm.mappingService.service.EmployeeIdMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mapping")
public class EmployeeIdMappingController {

    @Autowired
    private EmployeeIdMappingService service;

    // ✅ Create / update mapping
    @PostMapping("/map")
    public EmployeeIdMappingResponse mapEmployeeIds(@RequestBody EmployeeIdMappingRequest request) {
        EmployeeIdMapping mapping = service.mapEmployeeIds(
                request.getAttendanceEmpId(),
                request.getPayrollServiceUuid(),
                request.getLeaveServiceUuid(),
                request.getEmployeeServiceUuid(),
                request.getEmployeeName(),
                request.getEmployeeEmail(),
                request.getAnnualStructureId()
        );
        return service.convertToResponse(mapping);
    }


    // ✅ Fetch by Attendance ID
    @GetMapping("/attendance/{attendanceId}")
    public ResponseEntity<EmployeeIdMappingResponse> getByAttendanceId(@PathVariable String attendanceId) {
        Optional<EmployeeIdMapping> mapping = service.getByAttendanceId(attendanceId);
        return mapping.map(m -> ResponseEntity.ok(service.convertToResponse(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/payroll/{payrollUuid}")
    public ResponseEntity<EmployeeIdMappingResponse> getByPayrollUuid(@PathVariable String payrollUuid) {
        Optional<EmployeeIdMapping> mapping = service.getByPayrollUuid(payrollUuid);
        return mapping.map(m -> ResponseEntity.ok(service.convertToResponse(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/leave/{leaveUuid}")
    public ResponseEntity<EmployeeIdMappingResponse> getByLeaveUuid(@PathVariable String leaveUuid) {
        Optional<EmployeeIdMapping> mapping = service.getByLeaveUuid(leaveUuid);
        return mapping.map(m -> ResponseEntity.ok(service.convertToResponse(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeUuid}")
    public ResponseEntity<EmployeeIdMappingResponse> getByEmployeeUuid(@PathVariable String employeeUuid) {
        Optional<EmployeeIdMapping> mapping = service.getByEmployeeUuid(employeeUuid);
        return mapping.map(m -> ResponseEntity.ok(service.convertToResponse(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Get by Annual Structure ID
    @GetMapping("/annual-structure/{annualStructureId}")
    public ResponseEntity<EmployeeIdMappingResponse> getByAnnualStructureId(@PathVariable Long annualStructureId) {
        Optional<EmployeeIdMapping> mapping = service.getByAnnualStructureId(annualStructureId);
        return mapping.map(m -> ResponseEntity.ok(service.convertToResponse(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ List all
    @GetMapping("/all")
    public List<EmployeeIdMappingResponse> getAllMappings() {
        return service.getAllMappings().stream()
                .map(service::convertToResponse)
                .collect(Collectors.toList());
    }

    // ✅ Without annual structure
    @GetMapping("/without-annual-structure")
    public List<EmployeeIdMappingResponse> getMappingsWithoutAnnualStructure() {
        return service.getMappingsWithoutAnnualStructure().stream()
                .map(service::convertToResponse)
                .collect(Collectors.toList());
    }

    // ✅ With annual structure
    @GetMapping("/with-annual-structure")
    public List<EmployeeIdMappingResponse> getMappingsWithAnnualStructure() {
        return service.getMappingsWithAnnualStructure().stream()
                .map(service::convertToResponse)
                .collect(Collectors.toList());
    }

    // ✅ Search by employee name
    @GetMapping("/search")
    public List<EmployeeIdMappingResponse> searchByEmployeeName(@RequestParam String name) {
        return service.searchByEmployeeName(name).stream()
                .map(service::convertToResponse)
                .collect(Collectors.toList());
    }

    // ✅ Update annual structure for employee
    @PutMapping("/{employeeUuid}/annual-structure")
    public ResponseEntity<EmployeeIdMappingResponse> updateAnnualStructure(
            @PathVariable String employeeUuid,
            @RequestParam Long annualStructureId) {
        try {
            EmployeeIdMapping mapping = service.updateAnnualStructureId(employeeUuid, annualStructureId);
            return ResponseEntity.ok(service.convertToResponse(mapping));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Sync employees and attendance
    @PostMapping("/sync-employees")
    public ResponseEntity<Map<String, Object>> syncAll() {
        List<EmployeeIdMapping> updated = service.syncAllEmployeesAndAttendance();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Employees and attendance synced successfully");
        response.put("count", updated.size());
        response.put("data", updated.stream()
                .map(service::convertToResponse)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    // ✅ NEW: Get employee email by Leave UUID
    @GetMapping("/email/{leaveUuid}")
    public ResponseEntity<String> getEmployeeEmailByLeaveUuid(@PathVariable String leaveUuid) {
        String email = service.getEmployeeEmailByLeaveUuid(leaveUuid);
        if (email != null) {
            return ResponseEntity.ok(email);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}
