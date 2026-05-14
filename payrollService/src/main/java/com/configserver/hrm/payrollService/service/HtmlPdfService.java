package com.configserver.hrm.payrollService.service;

import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class HtmlPdfService {

    public byte[] generatePdfFromHtml(PayslipDTO payslip) throws Exception {

        // 1. Load HTML Template
        ClassPathResource resource = new ClassPathResource("templates/payslip.html");
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // 2. Placeholder replacement map
        Map<String, String> values = new HashMap<>();

        // === Employee Information ===
        values.put("employeeId", safe(payslip.getEmployeeId()));
        values.put("employeeName", safe(payslip.getEmployeeName()));
        values.put("designation", safe(payslip.getDesignation()));
        values.put("department", safe(payslip.getDepartment()));
        values.put("grade", safe(payslip.getGrade()));
        values.put("vendor", safe(payslip.getVendorCode()));
        values.put("joiningDate", payslip.getDateOfJoining() != null ? payslip.getDateOfJoining().toString() : "-");
        values.put("pfNo", safe(payslip.getPfNo()));
        values.put("bankNumber", safe(payslip.getBankAccountNo()));
        values.put("bankName", safe(payslip.getBankName()));
        values.put("branch", safe(payslip.getBankBranch()));
        values.put("panNo", safe(payslip.getPanNo()));
        values.put("uan", safe(payslip.getUanNo()));

        // === Attendance ===
        values.put("present", String.valueOf(payslip.getPresentDays()));
        values.put("holiday", String.valueOf(payslip.getHoliday()));
        values.put("paidLeave", String.valueOf(payslip.getPaidLeave()));
        values.put("weekOff", String.valueOf(payslip.getWeekOff()));
        values.put("outDuty", String.valueOf(payslip.getOutDuty()));
        values.put("unpaid", String.valueOf(payslip.getUnpaidLeave()));
        values.put("gatePass", String.valueOf(payslip.getGatePass()));
        values.put("salaryDays", String.valueOf(payslip.getTotalSalaryDays()));

        // === Salary ===
        values.put("basic", format(payslip.getBasicSalary()));
        values.put("extraDays", format(payslip.getExtraDaysWorked()));
        values.put("hra", format(payslip.getHouseRentAllowance()));
        values.put("education", format(payslip.getChildrenEducation()));
        values.put("otherB", format(payslip.getMonthlyOtherB()));
        values.put("performance", format(payslip.getPerformanceBonus()));
        values.put("special", format(payslip.getSpecialAllowance()));

        // === Deductions ===
        values.put("pt", format(payslip.getProfessionalTax()));
        values.put("employeePf", format(payslip.getPfAmount()));

        // === Totals ===
        values.put("totalEarnings", format(payslip.getTotalEarnings()));
        values.put("totalDeductions", format(payslip.getTotalDeductions()));
        values.put("netPay", format(payslip.getEmployeeNetPay()));

        // === Month/Period ===
        values.put("payDate", payslip.getMonth() != null ? payslip.getMonth().toString() : "-");

        // 3. Replace placeholders in HTML
        for (String key : values.keySet()) {
            html = html.replace("{{" + key + "}}", values.get(key));
        }

        // 4. Generate PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();

        return out.toByteArray();
    }

    private String safe(Object o) {
        return o == null ? "-" : o.toString();
    }

    private String format(double val) {
        return String.format("%.2f", val);
    }
}
