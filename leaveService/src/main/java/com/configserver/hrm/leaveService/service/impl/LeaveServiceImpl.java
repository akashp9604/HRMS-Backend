package com.configserver.hrm.leaveService.service.impl;

import com.configserver.hrm.leaveService.dto.HolidayDTO;
import com.configserver.hrm.leaveService.dto.LeaveResponseDTO;
import com.configserver.hrm.leaveService.entity.EmployeeLeave;
import com.configserver.hrm.leaveService.entity.LeaveBalance;
import com.configserver.hrm.leaveService.entity.LeaveStatus;
import com.configserver.hrm.leaveService.entity.LeaveType;
import com.configserver.hrm.leaveService.exception.InvalidLeaveDataException;
import com.configserver.hrm.leaveService.external.MappingServiceClient;
import com.configserver.hrm.leaveService.repository.EmployeeLeaveRepository;
import com.configserver.hrm.leaveService.repository.LeaveBalanceRepository;
import com.configserver.hrm.leaveService.service.HolidayService;
import com.configserver.hrm.leaveService.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Value("${hrm.holidays}")
    private String holidaysConfig;

    @Value("${hrm.leave.types}")
    private String leaveTypesConfig;

    @Value("${hrm.leave.monthly.paid.limit:2}")
    private int monthlyPaidLeaveLimit;

    @Autowired
    private EmployeeLeaveRepository leaveRepository;

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MappingServiceClient mappingServiceClient;

    // ==================== APPLY LEAVE ====================
    @Override
    @Transactional
    public EmployeeLeave applyLeave(String employeeId, String employeeName, String leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        String mappedId = resolveLeaveEmpId(employeeId);

        if (employeeName == null || employeeName.isBlank()) {
            employeeName = mappingServiceClient.getEmployeeName(mappedId);
        }

        if (endDate.isBefore(startDate)) {
            throw new InvalidLeaveDataException("End date cannot be before start date");
        }

        if (LeaveType.PAID.name().equals(leaveType) && !isSameMonth(startDate, endDate)) {
            throw new InvalidLeaveDataException("Paid leaves must be within the same month");
        }

        initializeLeaveBalances(mappedId);

        Set<LocalDate> holidays = holidayService.getHolidays().stream()
                .map(HolidayDTO::getDate)
                .collect(Collectors.toSet());

        boolean hasHoliday = startDate.datesUntil(endDate.plusDays(1))
                .anyMatch(holidays::contains);

        if (hasHoliday) {
            throw new InvalidLeaveDataException("Requested leave includes a holiday between " + startDate + " and " + endDate);
        }

        if (LeaveType.PAID.name().equals(leaveType)) {
            return handlePaidLeaveApplication(mappedId, employeeName, startDate, endDate, reason);
        }

        return applySingleLeave(mappedId, employeeName, LeaveType.valueOf(leaveType), startDate, endDate, reason, false, null);
    }

    private EmployeeLeave handlePaidLeaveApplication(String employeeId, String employeeName, LocalDate startDate, LocalDate endDate, String reason) {
        int currentMonth = startDate.getMonthValue();
        int currentYear = startDate.getYear();

        int paidLeavesUsed = getPaidLeavesUsedThisMonth(employeeId, currentMonth, currentYear);
        long requestedDays = startDate.datesUntil(endDate.plusDays(1)).count();

        if (paidLeavesUsed + requestedDays <= monthlyPaidLeaveLimit) {
            updatePaidLeaveCounter(employeeId, currentMonth, currentYear, (int) requestedDays);
            return applySingleLeave(employeeId, employeeName, LeaveType.PAID, startDate, endDate, reason, false, null);
        } else {
            int availablePaidDays = Math.max(0, monthlyPaidLeaveLimit - paidLeavesUsed);

            if (availablePaidDays > 0) {
                LocalDate paidEndDate = startDate.plusDays(availablePaidDays - 1);
                applySingleLeave(employeeId, employeeName, LeaveType.PAID, startDate, paidEndDate, reason, false, null);

                LocalDate unpaidStartDate = paidEndDate.plusDays(1);
                if (!unpaidStartDate.isAfter(endDate)) {
                    String unpaidReason = reason + " (Auto-converted from PAID)";
                    return applySingleLeave(employeeId, employeeName, LeaveType.UNPAID, unpaidStartDate, endDate,
                            unpaidReason, true, "Monthly paid leave limit exceeded");
                }
            } else {
                String unpaidReason = reason + " (Auto-converted from PAID)";
                return applySingleLeave(employeeId, employeeName, LeaveType.UNPAID, startDate, endDate,
                        unpaidReason, true, "Monthly paid leave limit exceeded");
            }
        }
        return getLatestLeave(employeeId);
    }

    private EmployeeLeave applySingleLeave(String employeeId, String employeeName, LeaveType leaveType, LocalDate startDate,
                                           LocalDate endDate, String reason, Boolean isConverted, String conversionReason) {

        if (leaveType != LeaveType.WFH && leaveType != LeaveType.UNPAID) {
            LeaveBalance balance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, leaveType)
                    .orElseThrow(() -> new InvalidLeaveDataException("No leave balance found for type: " + leaveType));

            long daysRequested = startDate.datesUntil(endDate.plusDays(1)).count();
            if (daysRequested > balance.getRemainingLeaves()) {
                throw new InvalidLeaveDataException("Insufficient leave balance. Remaining: " + balance.getRemainingLeaves());
            }
        }

        EmployeeLeave leave = new EmployeeLeave();
        leave.setEmployeeId(employeeId);
        leave.setEmployeeName(employeeName);
        leave.setLeaveType(leaveType);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);
        leave.setIsConvertedFromPaid(isConverted);
        leave.setConversionReason(conversionReason);

        if (leaveType == LeaveType.UNPAID && isConverted) {
            leave.setStatus(LeaveStatus.PENDING);
            leave.setReason(reason + " (Requires Manager Approval - Converted from PAID)");
        } else if (leaveType == LeaveType.WFH) {
            leave.setStatus(LeaveStatus.PENDING);
        } else {
            leave.setStatus(LeaveStatus.PENDING);
        }

        return leaveRepository.save(leave);
    }

    // ==================== APPROVE LEAVE ====================
    @Override
    @Transactional
    public EmployeeLeave approveLeave(UUID leaveId, String managerId) {
        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only pending leaves can be approved");
        }

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(managerId);
        leave.setApprovedOn(LocalDateTime.now());
        leaveRepository.save(leave);

        if (leave.getLeaveType() != LeaveType.UNPAID && leave.getLeaveType() != LeaveType.WFH) {
            LeaveBalance balance = balanceRepository
                    .findByEmployeeIdAndLeaveType(leave.getEmployeeId(), leave.getLeaveType())
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            long days = leave.getStartDate().datesUntil(leave.getEndDate().plusDays(1)).count();
            balance.setUsedLeaves(balance.getUsedLeaves() + (int) days);
            balance.setRemainingLeaves(balance.getRemainingLeaves() - (int) days);
            balanceRepository.save(balance);
        }
        return leave;
    }

    // ==================== REJECT LEAVE (UPDATED FOR WFH CONVERSION) ====================
    @Override
    @Transactional
    public EmployeeLeave rejectLeave(UUID leaveId, String managerId) {
        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only pending leaves can be rejected");
        }

        if (leave.getLeaveType() == LeaveType.WFH) {
            return handleWFHRejection(leave, managerId);
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(managerId);
        leave.setApprovedOn(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    private EmployeeLeave handleWFHRejection(EmployeeLeave wfhLeave, String managerId) {
        String employeeId = wfhLeave.getEmployeeId();
        LocalDate startDate = wfhLeave.getStartDate();
        int totalDays = (int) startDate.datesUntil(wfhLeave.getEndDate().plusDays(1)).count();

        WFHAConversionResult conversion = calculateWFHConversion(employeeId, startDate, totalDays);

        wfhLeave.setStatus(LeaveStatus.REJECTED);
        wfhLeave.setApprovedBy(managerId);
        wfhLeave.setApprovedOn(LocalDateTime.now());
        wfhLeave.setConvertedFromWFH(true);
        wfhLeave.setConvertedLeaveType(conversion.getConvertedLeaveType());
        wfhLeave.setConvertedPaidDays(conversion.getPaidDays());
        wfhLeave.setConvertedUnpaidDays(conversion.getUnpaidDays());

        deductBalancesForConversion(employeeId, conversion);

        return leaveRepository.save(wfhLeave);
    }

    private WFHAConversionResult calculateWFHConversion(String employeeId, LocalDate startDate, int totalDays) {
        int month = startDate.getMonthValue();
        int year = startDate.getYear();

        int paidLeavesUsed = getPaidLeavesUsedThisMonth(employeeId, month, year);
        int availablePaidDays = Math.max(0, monthlyPaidLeaveLimit - paidLeavesUsed);

        LeaveBalance unpaidBalance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, LeaveType.UNPAID)
                .orElseThrow(() -> new RuntimeException("UNPAID balance not found"));
        int availableUnpaidDays = unpaidBalance.getRemainingLeaves();

        int paidDays = Math.min(availablePaidDays, totalDays);
        int unpaidDays = totalDays - paidDays;

        if (unpaidDays > availableUnpaidDays) {
            throw new RuntimeException("Insufficient UNPAID balance. Available: " +
                    availableUnpaidDays + ", Required: " + unpaidDays);
        }

        LeaveType convertedType = (paidDays > 0 && unpaidDays > 0) ?
                LeaveType.PAID : (paidDays > 0) ? LeaveType.PAID : LeaveType.UNPAID;

        return new WFHAConversionResult(paidDays, unpaidDays, convertedType);
    }

    private void deductBalancesForConversion(String employeeId, WFHAConversionResult conversion) {
        if (conversion.getPaidDays() > 0) {
            LeaveBalance paidBalance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, LeaveType.PAID)
                    .orElseThrow(() -> new RuntimeException("PAID balance not found"));

            paidBalance.setUsedLeaves(paidBalance.getUsedLeaves() + conversion.getPaidDays());
            paidBalance.setRemainingLeaves(paidBalance.getRemainingLeaves() - conversion.getPaidDays());
            balanceRepository.save(paidBalance);

            updatePaidLeaveCounter(employeeId, LocalDate.now().getMonthValue(),
                    LocalDate.now().getYear(), conversion.getPaidDays());
        }

        if (conversion.getUnpaidDays() > 0) {
            LeaveBalance unpaidBalance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, LeaveType.UNPAID)
                    .orElseThrow(() -> new RuntimeException("UNPAID balance not found"));

            unpaidBalance.setUsedLeaves(unpaidBalance.getUsedLeaves() + conversion.getUnpaidDays());
            unpaidBalance.setRemainingLeaves(unpaidBalance.getRemainingLeaves() - conversion.getUnpaidDays());
            balanceRepository.save(unpaidBalance);
        }
    }

    // ==================== CANCEL LEAVE ====================
    @Override
    @Transactional
    public EmployeeLeave cancelLeave(UUID leaveId) {
        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING && leave.getStatus() != LeaveStatus.APPROVED) {
            throw new RuntimeException("Only pending or approved leaves can be cancelled");
        }

        if (leave.getStatus() == LeaveStatus.APPROVED) {
            restoreBalancesForCancellation(leave);
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        return leaveRepository.save(leave);
    }

    private void restoreBalancesForCancellation(EmployeeLeave leave) {
        if (leave.getConvertedFromWFH() && leave.getConvertedPaidDays() != null && leave.getConvertedUnpaidDays() != null) {
            if (leave.getConvertedPaidDays() > 0) {
                LeaveBalance paidBalance = balanceRepository.findByEmployeeIdAndLeaveType(leave.getEmployeeId(), LeaveType.PAID)
                        .orElseThrow(() -> new RuntimeException("PAID balance not found"));
                paidBalance.setUsedLeaves(paidBalance.getUsedLeaves() - leave.getConvertedPaidDays());
                paidBalance.setRemainingLeaves(paidBalance.getRemainingLeaves() + leave.getConvertedPaidDays());
                balanceRepository.save(paidBalance);
            }

            if (leave.getConvertedUnpaidDays() > 0) {
                LeaveBalance unpaidBalance = balanceRepository.findByEmployeeIdAndLeaveType(leave.getEmployeeId(), LeaveType.UNPAID)
                        .orElseThrow(() -> new RuntimeException("UNPAID balance not found"));
                unpaidBalance.setUsedLeaves(unpaidBalance.getUsedLeaves() - leave.getConvertedUnpaidDays());
                unpaidBalance.setRemainingLeaves(unpaidBalance.getRemainingLeaves() + leave.getConvertedUnpaidDays());
                balanceRepository.save(unpaidBalance);
            }
        }
        else if (leave.getLeaveType() != LeaveType.UNPAID && leave.getLeaveType() != LeaveType.WFH) {
            LeaveBalance balance = balanceRepository
                    .findByEmployeeIdAndLeaveType(leave.getEmployeeId(), leave.getLeaveType())
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            long days = leave.getStartDate().datesUntil(leave.getEndDate().plusDays(1)).count();
            balance.setUsedLeaves(balance.getUsedLeaves() - (int) days);
            balance.setRemainingLeaves(balance.getRemainingLeaves() + (int) days);

            if (leave.getLeaveType() == LeaveType.PAID) {
                balance.setPaidLeavesUsedThisMonth(Math.max(0, balance.getPaidLeavesUsedThisMonth() - (int) days));
            }

            balanceRepository.save(balance);
        }
    }

    // ==================== EXISTING GETTER METHODS ====================
    @Override
    public List<EmployeeLeave> getLeavesByEmployee(String employeeId) {
        return leaveRepository.findByEmployeeId(resolveLeaveEmpId(employeeId));
    }

    @Override
    public List<EmployeeLeave> getPendingLeaves() {
        return leaveRepository.findByStatus(LeaveStatus.PENDING);
    }

    @Override
    public List<LeaveResponseDTO> getAllLeaves() {
        return leaveRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<LeaveType, Integer> getAllRemainingLeaves(String employeeId) {
        String mappedId = resolveLeaveEmpId(employeeId);
        List<LeaveBalance> balances = balanceRepository.findByEmployeeId(mappedId);
        return balances.stream()
                .collect(Collectors.toMap(LeaveBalance::getLeaveType, LeaveBalance::getRemainingLeaves));
    }

    @Override
    public void initializeAllEmployeesFromAttendance() {
        // Implementation remains same
    }

    @Override
    public List<EmployeeLeave> getLeavesBetweenDates(String employeeId, LocalDate from, LocalDate to) {
        return leaveRepository.findByEmployeeIdAndStartDateBetween(resolveLeaveEmpId(employeeId), from, to);
    }

    @Override
    public Map<String, Long> getMonthlyLeaveSummary(String employeeId, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return leaveRepository.findByEmployeeIdAndStartDateBetween(resolveLeaveEmpId(employeeId), start, end)
                .stream()
                .filter(l -> LeaveStatus.APPROVED.equals(l.getStatus()))
                .collect(Collectors.groupingBy(l -> l.getLeaveType().name(), Collectors.counting()));
    }

    // ==================== HELPER METHODS ====================
    private int getPaidLeavesUsedThisMonth(String employeeId, int month, int year) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        return leaveRepository.findByEmployeeIdAndStartDateBetween(employeeId, monthStart, monthEnd)
                .stream()
                .filter(leave -> leave.getLeaveType() == LeaveType.PAID &&
                        (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.PENDING))
                .mapToInt(leave -> {
                    long days = leave.getStartDate().datesUntil(leave.getEndDate().plusDays(1)).count();
                    return (int) days;
                })
                .sum();
    }

    private void updatePaidLeaveCounter(String employeeId, int month, int year, int daysUsed) {
        Optional<LeaveBalance> paidBalance = balanceRepository.findByEmployeeIdAndLeaveType(employeeId, LeaveType.PAID);
        paidBalance.ifPresent(balance -> {
            balance.setPaidLeavesUsedThisMonth(balance.getPaidLeavesUsedThisMonth() + daysUsed);
            balance.setMonth(month);
            balance.setYear(year);
            balanceRepository.save(balance);
        });
    }

    private boolean isSameMonth(LocalDate startDate, LocalDate endDate) {
        return startDate.getMonth() == endDate.getMonth() && startDate.getYear() == endDate.getYear();
    }

    private void initializeLeaveBalances(String employeeId) {
        if (leaveTypesConfig == null || leaveTypesConfig.isEmpty()) return;

        String[] leaveTypes = leaveTypesConfig.split(",");
        for (String typePair : leaveTypes) {
            try {
                String[] parts = typePair.split(":");
                String typeName = parts[0];
                int defaultCount = Integer.parseInt(parts[1]);

                balanceRepository.findByEmployeeIdAndLeaveType(employeeId, LeaveType.valueOf(typeName))
                        .orElseGet(() -> {
                            LeaveBalance balance = new LeaveBalance();
                            balance.setEmployeeId(employeeId);
                            balance.setLeaveType(LeaveType.valueOf(typeName));
                            balance.setTotalLeaves(defaultCount);
                            balance.setUsedLeaves(0);
                            balance.setRemainingLeaves(defaultCount);
                            balance.setPaidLeavesUsedThisMonth(0);
                            balance.setMonth(LocalDate.now().getMonthValue());
                            balance.setYear(LocalDate.now().getYear());
                            return balanceRepository.save(balance);
                        });
            } catch (Exception e) {
                System.err.println("Failed to initialize leave balance for employee " +
                        employeeId + " and leave type " + typePair + ": " + e.getMessage());
            }
        }
    }

    private LeaveResponseDTO mapToDTO(EmployeeLeave leave) {
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
            String employeeName = mappingServiceClient.getEmployeeName(leave.getEmployeeId());
            dto.setEmployeeName(employeeName);
        } catch (Exception e) {
            dto.setEmployeeName("Unknown");
        }
        return dto;
    }

    private String resolveLeaveEmpId(String employeeId) {
        try {
            String mappedId = mappingServiceClient.getLeaveEmpUuid(employeeId);
            return mappedId != null ? mappedId : employeeId;
        } catch (Exception e) {
            return employeeId;
        }
    }

    private EmployeeLeave getLatestLeave(String employeeId) {
        return leaveRepository.findByEmployeeId(employeeId)
                .stream()
                .max(Comparator.comparing(EmployeeLeave::getAppliedOn))
                .orElse(null);
    }

    public long getWFHDaysCount(String employeeId, LocalDate from, LocalDate to) {
        return leaveRepository.findByEmployeeIdAndStartDateBetween(employeeId, from, to)
                .stream()
                .filter(leave -> leave.getLeaveType() == LeaveType.WFH &&
                        (leave.getStatus() == LeaveStatus.APPROVED))
                .mapToLong(leave -> leave.getStartDate().datesUntil(leave.getEndDate().plusDays(1)).count())
                .sum();
    }

    public List<EmployeeLeave> getConvertedLeaves(String employeeId) {
        return leaveRepository.findByEmployeeId(employeeId)
                .stream()
                .filter(leave -> leave.getIsConvertedFromPaid() || leave.getConvertedFromWFH())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getMonthlyPaidLeaveUsage(String employeeId, int month, int year) {
        int used = getPaidLeavesUsedThisMonth(employeeId, month, year);
        Map<String, Object> usage = new HashMap<>();
        usage.put("used", used);
        usage.put("limit", monthlyPaidLeaveLimit);
        usage.put("remaining", Math.max(0, monthlyPaidLeaveLimit - used));
        usage.put("month", month);
        usage.put("year", year);
        return usage;
    }

    private static class WFHAConversionResult {
        private final int paidDays;
        private final int unpaidDays;
        private final LeaveType convertedLeaveType;

        public WFHAConversionResult(int paidDays, int unpaidDays, LeaveType convertedLeaveType) {
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.convertedLeaveType = convertedLeaveType;
        }

        public int getPaidDays() { return paidDays; }
        public int getUnpaidDays() { return unpaidDays; }
        public LeaveType getConvertedLeaveType() { return convertedLeaveType; }
    }

    @Override
    public List<EmployeeLeave> getApprovedLeaves() {
        return leaveRepository.findByStatus(LeaveStatus.APPROVED);
    }
    @Override
    public List<EmployeeLeave> getApprovedLeavesByEmployee(String employeeId) {
        return leaveRepository.findByEmployeeIdAndStatus(employeeId, LeaveStatus.APPROVED);
    }

    @Override
    @Transactional
    public EmployeeLeave updateLeave(UUID leaveId, LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason) {

        return leaveRepository.findById(leaveId)
                .filter(leave -> leave.getStatus() == LeaveStatus.PENDING)
                .map(leave -> {
                    // Update fields
                    leave.setLeaveType(leaveType);
                    leave.setStartDate(startDate);
                    leave.setEndDate(endDate);
                    leave.setReason(reason);

                    // Reset approval fields
                    leave.setStatus(LeaveStatus.PENDING);
                    leave.setApprovedBy(null);
                    leave.setApprovedOn(null);
                    leave.setConvertedPaidDays(0);
                    leave.setConvertedUnpaidDays(0);

                    return leaveRepository.save(leave);
                })
                .orElseThrow(() -> new RuntimeException(
                        leaveRepository.findById(leaveId)
                                .map(l -> l.getStatus() == LeaveStatus.PENDING ?
                                        "Leave not found" : "Cannot edit leave that is already " + l.getStatus())
                                .orElse("Leave not found with id: " + leaveId)
                ));
    }

}