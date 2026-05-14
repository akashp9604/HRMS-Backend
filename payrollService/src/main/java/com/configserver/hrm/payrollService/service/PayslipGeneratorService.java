package com.configserver.hrm.payrollService.service;

public interface PayslipGeneratorService {

    /**
     * Generates a dynamic payslip PDF for an employee and returns it as bytes.
     *
     * @param employeeId the UUID or string ID of the employee
     * @param month      the month (1–12)
     * @param year       the year (e.g. 2025)
     * @return PDF file as byte array
     */
    byte[] generatePayslipPdf(String employeeId, int month, int year, String authHeader);
}
