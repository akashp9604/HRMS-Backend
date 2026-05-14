package com.configserver.hrm.payrollService.controller;

import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.configserver.hrm.payrollService.dto.PayrollSummaryDTO;
import com.configserver.hrm.payrollService.dto.AnnualSalaryStructureDTO;
import com.configserver.hrm.payrollService.service.PayrollService;
import com.configserver.hrm.payrollService.service.PayslipGeneratorService;
import com.configserver.hrm.payrollService.service.PdfGenerationService;
import com.itextpdf.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "http://localhost:3000")
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private PayslipGeneratorService payslipGeneratorService;


    // ==================== EXISTING ENDPOINTS ====================

    @PostMapping("/generate")
    public PayslipDTO generatePayslip(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam UUID employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        return payrollService.generatePayslip(employeeId, month, year, authHeader);
    }

    @GetMapping("/all")
    public List<PayslipDTO> getAllPayslips() {
        return payrollService.getAllPayslips();
    }

    @GetMapping("/summary")
    public PayrollSummaryDTO getPayrollSummary(
            @RequestParam int month,
            @RequestParam int year) {
        return payrollService.getPayrollSummary(month, year);
    }

    // ==================== NEW ANNUAL STRUCTURE ENDPOINTS ====================

    @PostMapping("/annual-structure")
    public AnnualSalaryStructureDTO createAnnualSalaryStructure(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam UUID employeeId) {
        return payrollService.createAnnualSalaryStructure(employeeId, authHeader);
    }

    @GetMapping("/annual-structure")
    public AnnualSalaryStructureDTO getAnnualSalaryStructure(
            @RequestParam UUID employeeId,
            @RequestParam String financialYear) {
        return payrollService.getAnnualSalaryStructure(employeeId, financialYear);
    }

    @GetMapping("/annual-structures")
    public List<AnnualSalaryStructureDTO> getAllAnnualStructures() {
        return payrollService.getAllAnnualStructures();
    }

    @GetMapping("/annual-structures/offer-letter")
    public List<AnnualSalaryStructureDTO> getStructuresForOfferLetter() {
        return payrollService.getAnnualStructuresForOfferLetter();
    }

    @PutMapping("/annual-structure/{structureId}/offer-letter")
    public AnnualSalaryStructureDTO markAddedToOfferLetter(
            @PathVariable Long structureId,
            @RequestParam String offerLetterId) {
        return payrollService.markAddedToOfferLetter(structureId, offerLetterId);
    }

    // ==================== NEW PDF DOWNLOAD ENDPOINTS ====================

    @GetMapping("/download-payslip/{payslipId}")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long payslipId) {
        try {
            byte[] pdfBytes = payrollService.generatePayslipPdf(payslipId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "payslip_" + payslipId + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (DocumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generating PDF: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }


    @GetMapping("/total-payroll")
    public Map<String, Object> getTotalPayroll(@RequestParam int month, @RequestParam int year) {
        double total = payrollService.calculateTotalPayroll(month, year);
        return Map.of(
                "month", month,
                "year", year,
                "totalPayroll", total
        );
    }
    @GetMapping("/payslip/count")
    public long getTotalPayslipCount() {
        return payrollService.countAllPayslips();
    }
    @GetMapping("/payslip/count/by-month")
    public long getPayslipCountByMonth(
            @RequestParam int month,
            @RequestParam int year) {
        return payrollService.countPayslipsByMonth(month, year);
    }

   /* @GetMapping("/download-template-payslip")
    public ResponseEntity<byte[]> downloadTemplatePayslip(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam UUID employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        try {
            byte[] pdfBytes = payslipGeneratorService.generatePayslipPdf(employeeId.toString(), month, year, authHeader);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = String.format("Payslip_Template_%s_%d_%d.pdf", employeeId, month, year);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generating payslip: " + e.getMessage()).getBytes());
        }
    }

*/
   @GetMapping("/download-payslip/by-month")
   public ResponseEntity<byte[]> downloadPayslipByMonth(
           @RequestParam UUID employeeId,
           @RequestParam int month,
           @RequestParam int year) {
       try {
           byte[] pdfBytes = payrollService.generatePayslipPdf(employeeId, month, year);

           HttpHeaders headers = new HttpHeaders();
           headers.setContentType(MediaType.APPLICATION_PDF);
           String filename = String.format("Payslip_%s_%d_%d.pdf", employeeId, month, year);
           headers.setContentDispositionFormData("attachment", filename);

           return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
       } catch (Exception e) {
           e.printStackTrace();
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(("Error generating payslip: " + e.getMessage()).getBytes());
       }
   }

}