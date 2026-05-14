package com.configserver.hrm.leaveService.repository;

import com.configserver.hrm.leaveService.entity.EmployeeLeave;
import com.configserver.hrm.leaveService.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, UUID> {
    List<EmployeeLeave> findByEmployeeId(String employeeId);
    List<EmployeeLeave> findByEmployeeIdAndStartDateBetween(String employeeId, LocalDate from, LocalDate to);
    List<EmployeeLeave> findByStatus(LeaveStatus status);
    List<EmployeeLeave> findByEmployeeIdAndStatus(String employeeId, LeaveStatus status);

}
