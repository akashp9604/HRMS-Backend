package com.configserver.hrm.payrollService.repository;

import com.configserver.hrm.payrollService.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByMonth(LocalDate month);
}
