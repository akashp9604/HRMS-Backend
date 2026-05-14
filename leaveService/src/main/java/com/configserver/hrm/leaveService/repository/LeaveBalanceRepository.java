package com.configserver.hrm.leaveService.repository;

import com.configserver.hrm.leaveService.entity.LeaveBalance;
import com.configserver.hrm.leaveService.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, String> {

    // Find leave balance by employee and type
    Optional<LeaveBalance> findByEmployeeIdAndLeaveType(String employeeId, LeaveType leaveType);

    // Get all leave balances for an employee
    List<LeaveBalance> findByEmployeeId(String employeeId);
}
