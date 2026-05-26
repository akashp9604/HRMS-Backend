package com.configserver.hrm.leaveService.service;
import com.configserver.hrm.leaveService.dto.LeaveResponseDTO;
import com.configserver.hrm.leaveService.entity.EmployeeLeave;
import com.configserver.hrm.leaveService.entity.LeaveType;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LeaveService {

    EmployeeLeave applyLeave(String employeeId, String employeeName, String leaveType, LocalDate startDate, LocalDate endDate, String reason);
    EmployeeLeave applyLeaveWithDocument(String employeeId, String employeeName, String leaveType, LocalDate startDate, LocalDate endDate, String reason, MultipartFile document);
    EmployeeLeave approveLeave(UUID leaveId, String managerId) ;

    EmployeeLeave rejectLeave(UUID leaveId, String managerId);
    EmployeeLeave cancelLeave(UUID leaveId);
    List<EmployeeLeave> getLeavesByEmployee(String employeeId);
    //List<EmployeeLeave> getAllLeaves();
     Map<LeaveType, Integer> getAllRemainingLeaves(String employeeId);
    void initializeAllEmployeesFromAttendance();
    List<EmployeeLeave> getPendingLeaves();
    List<EmployeeLeave> getLeavesBetweenDates(String employeeId, LocalDate from, LocalDate to);
    Map<String, Long> getMonthlyLeaveSummary(String employeeId, int month, int year);
    List<LeaveResponseDTO> getAllLeaves();
    List<EmployeeLeave> getApprovedLeaves();
    List<EmployeeLeave> getApprovedLeavesByEmployee(String employeeId);
    // Edit/Update existing leave
    EmployeeLeave updateLeave(UUID leaveId, LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason);


}
