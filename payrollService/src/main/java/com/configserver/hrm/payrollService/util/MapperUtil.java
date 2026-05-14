package com.configserver.hrm.payrollService.util;

import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.configserver.hrm.payrollService.entity.Payslip;

public class MapperUtil {

    public static PayslipDTO toDTO(Payslip payslip) {
        if (payslip == null) return null;

        return PayslipDTO.builder()
                .employeeId(payslip.getEmployeeId())
                .employeeName(payslip.getEmployeeName())
                .designation(payslip.getDesignation())
                .department(payslip.getDepartment())
                .grade(payslip.getGrade())
                .vendorCode(payslip.getVendorCode())
                .dateOfJoining(payslip.getDateOfJoining())
                .pfNo(payslip.getPfNo())
                .bankAccountNo(payslip.getBankAccountNo())
                .bankName(payslip.getBankName())
                .bankBranch(payslip.getBankBranch())
                .panNo(payslip.getPanNo())
                .uanNo(payslip.getUanNo())
                .presentDays(payslip.getPresentDays())
                .holiday(payslip.getHoliday())
                .paidLeave(payslip.getPaidLeave())
                .gatePass(payslip.getGatePass())
                .weekOff(payslip.getWeekOff())
                .totalSalaryDays(payslip.getTotalSalaryDays())
                .outDuty(payslip.getOutDuty())
                .payslipMonth(payslip.getPayslipMonth())
                .payslipYear(payslip.getPayslipYear())
                .unpaidLeave(payslip.getUnpaidLeave())
                .totalWorkHours(payslip.getTotalWorkHours())
                .calculationMode(payslip.getCalculationMode())
                .basicSalary(payslip.getBasicSalary())
                .extraDaysWorked(payslip.getExtraDaysWorked())
                .houseRentAllowance(payslip.getHouseRentAllowance())
                .childrenEducation(payslip.getChildrenEducation())
                .monthlyOtherB(payslip.getMonthlyOtherB())
                .performanceBonus(payslip.getPerformanceBonus())
                .specialAllowance(payslip.getSpecialAllowance())
                .totalEarnings(payslip.getTotalEarnings())
                .totalDeductions(payslip.getTotalDeductions())
                .employeeNetPay(payslip.getEmployeeNetPay())
                .month(payslip.getMonth())
                .status(payslip.getStatus())
                .basic(payslip.getBasicSalary())
                .hra(payslip.getHouseRentAllowance())
                .allowances(payslip.getSpecialAllowance())
                .grossSalary(payslip.getTotalEarnings())
                .professionalTax(payslip.getProfessionalTax())
                .pfAmount(payslip.getEmployeePf())
                .deductions(payslip.getTotalDeductions())
                .netSalary(payslip.getEmployeeNetPay())
                .build();
    }
}
