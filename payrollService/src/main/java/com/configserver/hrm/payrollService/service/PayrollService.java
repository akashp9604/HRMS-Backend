package com.configserver.hrm.payrollService.service;

import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.configserver.hrm.payrollService.dto.PayrollSummaryDTO;
import com.configserver.hrm.payrollService.dto.AnnualSalaryStructureDTO;
import com.itextpdf.text.DocumentException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PayrollService {
    // Existing methods
    PayslipDTO generatePayslip(UUID employeeId, int month, int year, String authHeader);
    List<PayslipDTO> getAllPayslips();
    PayrollSummaryDTO getPayrollSummary(int month, int year);

    // Annual Salary Structure methods
    AnnualSalaryStructureDTO createAnnualSalaryStructure(UUID employeeId, String authHeader);
    AnnualSalaryStructureDTO getAnnualSalaryStructure(UUID employeeId, String financialYear);
    List<AnnualSalaryStructureDTO> getAnnualStructuresForOfferLetter();
    AnnualSalaryStructureDTO markAddedToOfferLetter(Long structureId, String offerLetterId);
    List<AnnualSalaryStructureDTO> getAllAnnualStructures();

    // NEW: PDF Generation methods
    byte[] generatePayslipPdf(Long payslipId) throws DocumentException;
    byte[] generatePayslipPdf(UUID employeeId, int month, int year) throws DocumentException;
    double calculateTotalPayroll(int month, int year);
    long countAllPayslips();
    long countPayslipsByMonth(int month, int year);

}