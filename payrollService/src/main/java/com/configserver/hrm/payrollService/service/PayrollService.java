package com.configserver.hrm.payrollService.service;

import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.configserver.hrm.payrollService.dto.PayrollSummaryDTO;
import com.configserver.hrm.payrollService.dto.AnnualSalaryStructureDTO;
import com.itextpdf.text.DocumentException;

import java.util.List;

public interface PayrollService {
    // Existing methods
    PayslipDTO generatePayslip(Long employeeId, int month, int year, String authHeader);
    List<PayslipDTO> getAllPayslips();
    PayrollSummaryDTO getPayrollSummary(int month, int year);

    // Annual Salary Structure methods
    AnnualSalaryStructureDTO createAnnualSalaryStructure(Long employeeId, String authHeader);
    AnnualSalaryStructureDTO getAnnualSalaryStructure(Long employeeId, String financialYear);
    List<AnnualSalaryStructureDTO> getAnnualStructuresForOfferLetter();
    AnnualSalaryStructureDTO markAddedToOfferLetter(Long structureId, String offerLetterId);
    List<AnnualSalaryStructureDTO> getAllAnnualStructures();

    // NEW: PDF Generation methods
//  byte[] generatePayslipPdf(Long payslipId) throws DocumentException;
//   byte[] generatePayslipPdf(Long employeeId, int month, int year) throws DocumentException;
//    double calculateTotalPayroll(int month, int year);
//    long countAllPayslips();
//    long countPayslipsByMonth(int month, int year);

    byte[] generatePayslipPdf(Long payslipId) throws DocumentException;
    byte[] generatePayslipPdf(Long employeeId, int month, int year, String authHeader) throws DocumentException;
    double calculateTotalPayroll(int month, int year);
    long countAllPayslips();
    long countPayslipsByMonth(int month, int year);
}