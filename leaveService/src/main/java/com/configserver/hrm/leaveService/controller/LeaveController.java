package com.configserver.hrm.leaveService.controller;

import com.configserver.hrm.leaveService.dto.LeaveRequestDTO;
import com.configserver.hrm.leaveService.dto.LeaveResponseDTO;
import com.configserver.hrm.leaveService.entity.EmployeeLeave;
import com.configserver.hrm.leaveService.entity.LeaveType;
import com.configserver.hrm.leaveService.external.MappingServiceClient;
import com.configserver.hrm.leaveService.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "http://localhost:3000")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private MappingServiceClient mappingServiceClient;

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@RequestBody EmployeeLeave leaveRequest) {
        try {
            String employeeId = leaveRequest.getEmployeeId();

            String employeeName = mappingServiceClient.getEmployeeName(employeeId);
            String leaveType = String.valueOf(leaveRequest.getLeaveType());
            LocalDate startDate = leaveRequest.getStartDate();
            LocalDate endDate = leaveRequest.getEndDate();
            String reason = leaveRequest.getReason();

            // Call service with 6 parameters including employeeName
            EmployeeLeave leave = leaveService.applyLeave(employeeId, employeeName, leaveType, startDate, endDate, reason);

            // Convert to DTO to include employee name
            LeaveResponseDTO responseDTO = convertToLeaveResponseDTO(leave);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }

    // Add this helper method to your LeaveController
    private LeaveResponseDTO convertToLeaveResponseDTO(EmployeeLeave leave) {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setId(leave.getId());
        dto.setEmployeeId(leave.getEmployeeId());
        dto.setLeaveType(leave.getLeaveType().name());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setStatus(leave.getStatus());
        dto.setReason(leave.getReason());
        dto.setApprovedBy(leave.getApprovedBy());
        dto.setAppliedOn(leave.getAppliedOn());
        dto.setApprovedOn(leave.getApprovedOn());

        try {
            // Get employee name from mapping service
            String employeeName = mappingServiceClient.getEmployeeName(leave.getEmployeeId());
            dto.setEmployeeName(employeeName);
        } catch (Exception e) {
            dto.setEmployeeName("Unknown");
        }

        return dto;
    }

    // NEW: Apply multiple leaves for calendar selection
    @PostMapping("/apply-multiple")
    public ResponseEntity<?> applyMultipleLeaves(@RequestBody List<LeaveRequestDTO> leaveRequests) {
        try {
            List<EmployeeLeave> appliedLeaves = leaveRequests.stream()
                    .map(request -> {
                        try {
                            // Get employee name first
                            String employeeName = mappingServiceClient.getEmployeeName(request.getEmployeeId().toString());

                            // Call service with 6 parameters including employeeName
                            return leaveService.applyLeave(
                                    request.getEmployeeId().toString(),
                                    employeeName,  // Add employee name as 2nd parameter
                                    request.getLeaveType(),
                                    request.getStartDate(),
                                    request.getEndDate(),
                                    request.getReason()
                            );
                        } catch (RuntimeException e) {
                            // Create a failed leave record for tracking
                            EmployeeLeave failedLeave = new EmployeeLeave();
                            failedLeave.setEmployeeId(request.getEmployeeId().toString());
                            failedLeave.setLeaveType(LeaveType.valueOf(request.getLeaveType()));
                            failedLeave.setStartDate(request.getStartDate());
                            failedLeave.setEndDate(request.getEndDate());
                            failedLeave.setReason("FAILED: " + e.getMessage());
                            return failedLeave;
                        }
                    })
                    .collect(Collectors.toList());

            // Convert to DTOs with employee names
            List<LeaveResponseDTO> responseDTOs = appliedLeaves.stream()
                    .map(this::convertToLeaveResponseDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                    "appliedLeaves", responseDTOs,  // Now returns DTOs with names
                    "successCount", appliedLeaves.stream().filter(l -> !l.getReason().startsWith("FAILED")).count(),
                    "failedCount", appliedLeaves.stream().filter(l -> l.getReason().startsWith("FAILED")).count()
            );

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }

    @PutMapping("/approve/{leaveId}")
    public ResponseEntity<?> approveLeave(
            @PathVariable String leaveId,
            @RequestParam String managerId) {
        try {
            UUID leaveUuid = UUID.fromString(leaveId);
            EmployeeLeave approvedLeave = leaveService.approveLeave(leaveUuid, managerId);
            return ResponseEntity.ok(approvedLeave);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }

    @PutMapping("/reject/{leaveId}")
    public ResponseEntity<?> rejectLeave(@PathVariable String leaveId, @RequestParam String managerId) {
        try {
            UUID leaveUuid = UUID.fromString(leaveId);
            EmployeeLeave rejectedLeave = leaveService.rejectLeave(leaveUuid, managerId);
            return ResponseEntity.ok(rejectedLeave);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }

    @PutMapping("/cancel/{leaveId}")
    public ResponseEntity<?> cancelLeave(@PathVariable String leaveId) {
        try {
            UUID leaveUuid = UUID.fromString(leaveId);
            EmployeeLeave cancelledLeave = leaveService.cancelLeave(leaveUuid);
            return ResponseEntity.ok(cancelledLeave);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + ex.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveResponseDTO>> getLeavesByEmployee(@PathVariable String employeeId) {
        try {
            List<EmployeeLeave> employeeLeaves = leaveService.getLeavesByEmployee(employeeId);
            // Convert to DTOs with employee names
            List<LeaveResponseDTO> responseDTOs = employeeLeaves.stream()
                    .map(this::convertToLeaveResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseDTOs);
        } catch (Exception e) {
            System.out.println("❌ Error fetching leaves for employee " + employeeId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<EmployeeLeave>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @GetMapping("/all")
    public List<LeaveResponseDTO> getAllLeaves() {
        return leaveService.getAllLeaves();
    }

    @GetMapping("/leave-balance/{employeeId}")
    public ResponseEntity<?> getLeaveBalances(@PathVariable String employeeId) {
        try {
            Map<LeaveType, Integer> balances = leaveService.getAllRemainingLeaves(employeeId);
            return ResponseEntity.ok(balances);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // NEW: Get monthly paid leave usage
    @GetMapping("/employee/{employeeId}/paid-leave-usage")
    public ResponseEntity<?> getMonthlyPaidLeaveUsage(
            @PathVariable String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        try {
            Map<String, Object> usage = ((com.configserver.hrm.leaveService.service.impl.LeaveServiceImpl) leaveService)
                    .getMonthlyPaidLeaveUsage(employeeId, month, year);
            return ResponseEntity.ok(usage);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // NEW: Get converted leaves
    @GetMapping("/employee/{employeeId}/converted-leaves")
    public ResponseEntity<List<EmployeeLeave>> getConvertedLeaves(@PathVariable String employeeId) {
        List<EmployeeLeave> convertedLeaves = ((com.configserver.hrm.leaveService.service.impl.LeaveServiceImpl) leaveService)
                .getConvertedLeaves(employeeId);
        return ResponseEntity.ok(convertedLeaves);
    }

    // NEW: Get WFH days count for payroll
    @GetMapping("/employee/{employeeId}/wfh-days")
    public ResponseEntity<Long> getWFHDaysCount(
            @PathVariable String employeeId,
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        long wfhDays = ((com.configserver.hrm.leaveService.service.impl.LeaveServiceImpl) leaveService)
                .getWFHDaysCount(employeeId, fromDate, toDate);
        return ResponseEntity.ok(wfhDays);
    }

    @PostMapping("/init-from-attendance")
    public ResponseEntity<String> initializeLeaveBalances() {
        leaveService.initializeAllEmployeesFromAttendance();
        return new ResponseEntity<>("Leave balances initialized from attendance service", HttpStatus.OK);
    }

    @GetMapping("/employee/{employeeId}/between")
    public ResponseEntity<List<Map<String, String>>> getLeavesBetweenDates(
            @PathVariable String employeeId,
            @RequestParam String from,
            @RequestParam String to) {

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        List<EmployeeLeave> leaves = leaveService.getLeavesBetweenDates(employeeId, fromDate, toDate);

        List<Map<String, String>> response = leaves.stream().map(leave -> Map.of(
                "date", leave.getStartDate().toString(),
                "leaveType", leave.getLeaveType().name(),
                "status", leave.getStatus().name(),
                "isWFH", String.valueOf(leave.getLeaveType() == LeaveType.WFH),
                "isConverted", String.valueOf(leave.getIsConvertedFromPaid())
        )).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{employeeId}/monthly-summary")
    public ResponseEntity<Map<String, Long>> getMonthlyLeaveSummary(
            @PathVariable String employeeId,
            @RequestParam int month,
            @RequestParam int year) {

        Map<String, Long> summary = leaveService.getMonthlyLeaveSummary(employeeId, month, year);
        return ResponseEntity.ok(summary);
    }
    @GetMapping("/approved")
    public ResponseEntity<List<LeaveResponseDTO>> getApprovedLeaves() {
        List<EmployeeLeave> approvedLeaves = leaveService.getApprovedLeaves();

        List<LeaveResponseDTO> responseDTOs = approvedLeaves.stream()
                .map(this::convertToLeaveResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOs);
    }
    @GetMapping("/employee/{employeeId}/approved")
    public ResponseEntity<List<LeaveResponseDTO>> getApprovedLeavesByEmployee(@PathVariable String employeeId) {
        try {
            List<EmployeeLeave> approvedLeaves = leaveService.getApprovedLeavesByEmployee(employeeId);

            // Convert to DTO with employee name included
            List<LeaveResponseDTO> responseDTOs = approvedLeaves.stream()
                    .map(this::convertToLeaveResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
    @GetMapping("/employee/{employeeId}/approved-between")
    public ResponseEntity<List<LeaveResponseDTO>> getApprovedLeavesBetweenDates(
            @PathVariable String employeeId,
            @RequestParam String from,
            @RequestParam String to) {

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        // Already existing service call
        List<EmployeeLeave> leaves = leaveService.getLeavesBetweenDates(employeeId, fromDate, toDate);

        // Just filter APPROVED here → No new service/repository needed
        List<EmployeeLeave> approvedLeaves = leaves.stream()
                .filter(l -> l.getStatus().name().equals("APPROVED"))
                .toList();

        // Convert to DTO
        List<LeaveResponseDTO> responseDTOs = approvedLeaves.stream()
                .map(this::convertToLeaveResponseDTO)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }


}