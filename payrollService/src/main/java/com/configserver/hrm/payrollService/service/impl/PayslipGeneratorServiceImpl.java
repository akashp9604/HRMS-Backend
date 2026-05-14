package com.configserver.hrm.payrollService.service.impl;

import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.configserver.hrm.payrollService.exception.PayrollException;
import com.configserver.hrm.payrollService.service.HtmlPdfService;
import com.configserver.hrm.payrollService.service.PayslipGeneratorService;
import com.configserver.hrm.payrollService.service.PayrollService;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

@Service
public class PayslipGeneratorServiceImpl implements PayslipGeneratorService {

    @Autowired
    private PayrollService payrollService;



    @Override
    public byte[] generatePayslipPdf(String employeeId, int month, int year, String authHeader) {
        try {
            UUID empUuid = UUID.fromString(employeeId);
            YearMonth ym = YearMonth.of(year, month);

            // 1️⃣ Get dynamic payslip data
            PayslipDTO payslipData = payrollService.generatePayslip(empUuid, month, year, authHeader);

            // 2️⃣ Load your PDF template with fields
            InputStream templateStream = getClass().getResourceAsStream("/templates/payslip_template.pdf");
            if (templateStream == null) {
                throw new FileNotFoundException("Payslip template not found at /templates/payslip_template.pdf");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfReader reader = new PdfReader(templateStream);
            PdfStamper stamper = new PdfStamper(reader, baos);
            AcroFields form = stamper.getAcroFields();

            // 3️⃣ Fill the PDF fields (names must match template)
            form.setField("employeeName", safe(payslipData.getEmployeeName()));
            form.setField("department", safe(payslipData.getDepartment()));
            form.setField("designation", safe(payslipData.getDesignation()));
            form.setField("grade", safe(payslipData.getGrade()));
            form.setField("month", ym.getMonth().toString() + " " + year);
            form.setField("basic", format(payslipData.getBasic()));
            form.setField("hra", format(payslipData.getHra()));
            form.setField("allowances", format(payslipData.getAllowances()));
            form.setField("deductions", format(payslipData.getDeductions()));
            form.setField("netSalary", format(payslipData.getNetSalary()));

            // 4️⃣ Flatten (make non-editable)
            stamper.setFormFlattening(true);
            stamper.close();
            reader.close();

            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new PayrollException("Error generating payslip PDF: " + e.getMessage());
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String format(Object value) {
        if (value == null) return "";
        if (value instanceof Number) return String.format("%.2f", ((Number) value).doubleValue());
        return value.toString();
    }
}
