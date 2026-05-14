package com.configserver.hrm.payrollService.service.impl;

import com.configserver.hrm.payrollService.client.AttendanceClient;
import com.configserver.hrm.payrollService.client.EmployeeClient;
import com.configserver.hrm.payrollService.client.LeaveClient;
import com.configserver.hrm.payrollService.client.MappingServiceClient;
import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.configserver.hrm.payrollService.dto.PayrollSummaryDTO;
import com.configserver.hrm.payrollService.dto.AnnualSalaryStructureDTO;
import com.configserver.hrm.payrollService.entity.Payslip;
import com.configserver.hrm.payrollService.entity.AnnualSalaryStructure;
import com.configserver.hrm.payrollService.repository.PayslipRepository;
import com.configserver.hrm.payrollService.repository.AnnualSalaryStructureRepository;
import com.configserver.hrm.payrollService.exception.PayrollException;
import com.configserver.hrm.payrollService.service.HtmlPdfService;
import com.configserver.hrm.payrollService.service.PayrollService;
import com.configserver.hrm.payrollService.service.PdfGenerationService;
import com.configserver.hrm.payrollService.util.MapperUtil;
import com.itextpdf.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollServiceImpl implements PayrollService {

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private AnnualSalaryStructureRepository annualStructureRepository;

    @Autowired
    private MappingServiceClient mappingServiceClient;

    @Autowired
    private AttendanceClient attendanceClient;

    @Autowired
    private LeaveClient leaveClient;

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private HtmlPdfService htmlPdfService;

    // ==================== ANNUAL SALARY STRUCTURE METHODS ====================

    @Override
    @Transactional
    public AnnualSalaryStructureDTO createAnnualSalaryStructure(UUID employeeId, String authHeader) {
        try {
            // 1. Fetch employee data
            Map<String, Object> employeeData = employeeClient.getEmployeePackage(employeeId, authHeader);
            String employeeName = (String) employeeData.getOrDefault("name", "Unknown");
            String designation = (String) employeeData.getOrDefault("designation", "Employee");
            double annualSalary = employeeData.get("annualSalary") != null
                    ? Double.parseDouble(employeeData.get("annualSalary").toString())
                    : 0.0;

            if (annualSalary <= 0) {
                throw new PayrollException("Annual salary must be greater than 0");
            }

            // 2. Calculate components using YOUR EXACT FORMULA
            // Monthly CTC = Annual CTC / 12
            double monthlyCtc = annualSalary / 12;

            // Basic = 40% of monthly CTC
            double monthlyBasic = monthlyCtc * 0.40;

            // HRA = 40% of Basic
            double monthlyHra = monthlyBasic * 0.40;

            // Pending amount after Basic and HRA
            double pending = monthlyCtc - (monthlyBasic + monthlyHra);

            // Employer PF = 12% of Basic (company side)
            double employerPf = monthlyBasic * 0.12;

            // Gratuity = Basic * 4.86%
            double gratuity = monthlyBasic * 0.0486;

            // Special Component = Pending - Employer PF - Gratuity
            double monthlySpecialComponent = pending - employerPf - gratuity;

            // Monthly Gross = Basic + HRA + Special Component
            double monthlyGross = monthlyBasic + monthlyHra + monthlySpecialComponent;

            // 🔴 UPDATED: Calculate deductions and net salary
            // Employee PF = 12% of Basic (employee side)
            double employeePf = monthlyBasic * 0.12;

            // Professional Tax (fixed)
            double professionalTax = 200.0;

            // Monthly Net Salary = Gross - Deductions
            double monthlyNet = monthlyGross - employeePf - professionalTax;

            // Convert to annual amounts
            double annualBasic = monthlyBasic * 12;
            double annualHra = monthlyHra * 12;
            double annualAllowances = monthlySpecialComponent * 12;
            double annualGross = monthlyGross * 12;

            // 🔴 NEW: Annual Net Salary
            double annualNet = monthlyNet * 12;

            System.out.println("💰 YOUR EXACT FORMULA CALCULATION:");
            System.out.println("  - Annual Package: ₹" + String.format("%,.2f", annualSalary));
            System.out.println("  - Monthly CTC: ₹" + String.format("%,.2f", monthlyCtc));
            System.out.println("  - Basic (40% of CTC): ₹" + String.format("%,.2f", monthlyBasic));
            System.out.println("  - HRA (40% of Basic): ₹" + String.format("%,.2f", monthlyHra));
            System.out.println("  - Pending: ₹" + String.format("%,.2f", pending));
            System.out.println("  - Employer PF: ₹" + String.format("%,.2f", employerPf));
            System.out.println("  - Gratuity: ₹" + String.format("%,.2f", gratuity));
            System.out.println("  - Special Component: ₹" + String.format("%,.2f", monthlySpecialComponent));
            System.out.println("  - Monthly Gross: ₹" + String.format("%,.2f", monthlyGross));
            System.out.println("  - Employee PF: ₹" + String.format("%,.2f", employeePf));
            System.out.println("  - Professional Tax: ₹" + professionalTax);
            System.out.println("  - Monthly Net Salary: ₹" + String.format("%,.2f", monthlyNet));
            System.out.println("  - Annual Net Salary: ₹" + String.format("%,.2f", annualNet));

            // 3. Validate calculation matches your example
            if (Math.abs(annualSalary - 1200000) < 1.0) {
                System.out.println("✅ VALIDATION FOR ₹12,00,000 PACKAGE:");
                System.out.println("  - Expected Monthly Gross: ₹93,256.00");
                System.out.println("  - Calculated Monthly Gross: ₹" + String.format("%,.2f", monthlyGross));
                System.out.println("  - Expected Net Salary: ₹88,256.00");
                System.out.println("  - Calculated Net Salary: ₹" + String.format("%,.2f", monthlyNet));
            }

            // 4. Determine financial year
            String financialYear = getCurrentFinancialYear();

            // 5. Check if structure already exists
            Optional<AnnualSalaryStructure> existingStructure =
                    annualStructureRepository.findByEmployeeIdAndFinancialYear(employeeId, financialYear);

            if (existingStructure.isPresent()) {
                throw new PayrollException("Annual salary structure already exists for employee in " + financialYear);
            }

            // 6. Create and save annual structure
            AnnualSalaryStructure structure = AnnualSalaryStructure.builder()
                    .employeeId(employeeId)
                    .employeeName(employeeName)
                    .designation(designation)
                    .annualBasic(annualBasic)
                    .annualHra(annualHra)
                    .annualAllowances(annualAllowances)
                    .annualGross(annualGross)
                    .annualNet(annualNet) // 🔴 NEW
                    .monthlyBasic(monthlyBasic)
                    .monthlyHra(monthlyHra)
                    .monthlyAllowances(monthlySpecialComponent) // This is now Special Component
                    .monthlyGross(monthlyGross)
                    .monthlyNet(monthlyNet) // 🔴 NEW
                    .financialYear(financialYear)
                    .status("DRAFT")
                    .addedToOfferLetter(false)
                    .build();

            AnnualSalaryStructure savedStructure = annualStructureRepository.save(structure);

            return convertToAnnualDTO(savedStructure);

        } catch (Exception e) {
            throw new PayrollException("Error creating annual salary structure: " + e.getMessage());
        }
    }

    @Override
    public AnnualSalaryStructureDTO getAnnualSalaryStructure(UUID employeeId, String financialYear) {
        AnnualSalaryStructure structure = annualStructureRepository
                .findByEmployeeIdAndFinancialYear(employeeId, financialYear)
                .orElseThrow(() -> new PayrollException("Annual salary structure not found for employee in " + financialYear));
        return convertToAnnualDTO(structure);
    }

    @Override
    public List<AnnualSalaryStructureDTO> getAnnualStructuresForOfferLetter() {
        return annualStructureRepository.findByAddedToOfferLetter(false)
                .stream()
                .map(this::convertToAnnualDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AnnualSalaryStructureDTO markAddedToOfferLetter(Long structureId, String offerLetterId) {
        AnnualSalaryStructure structure = annualStructureRepository.findById(structureId)
                .orElseThrow(() -> new PayrollException("Salary structure not found"));

        structure.setAddedToOfferLetter(true);
        structure.setOfferLetterId(offerLetterId);
        structure.setOfferLetterDate(LocalDate.now().atStartOfDay());
        structure.setStatus("APPROVED");

        AnnualSalaryStructure updated = annualStructureRepository.save(structure);
        return convertToAnnualDTO(updated);
    }

    @Override
    public List<AnnualSalaryStructureDTO> getAllAnnualStructures() {
        return annualStructureRepository.findAll()
                .stream()
                .map(this::convertToAnnualDTO)
                .collect(Collectors.toList());
    }

    // ==================== FIXED PAYSLIP GENERATION WITH AUTO-CREATION ====================

    @Override
    @Transactional
    public PayslipDTO generatePayslip(UUID employeeId, int month, int year, String authHeader) {
        try {
            // 1️⃣ Fetch Employee details dynamically
            Map<String, Object> empDetails = employeeClient.getEmployeeDetails(employeeId, authHeader);
            if (empDetails == null) {
                throw new PayrollException("Employee details not found for ID: " + employeeId);
            }

            // Map employee fields with null-safe defaults
            String employeeName = (String) empDetails.getOrDefault("name", "Unknown");
            String designation = (String) empDetails.getOrDefault("designation", "NA");
            String department = (String) empDetails.getOrDefault("department", "NA");
            String grade = (String) empDetails.getOrDefault("grade", "G3");
            String vendorCode = (String) empDetails.getOrDefault("vendorCode", "NA");

            String panNo = (String) empDetails.getOrDefault("panNumber", "NA");
            String pfNo = (String) empDetails.getOrDefault("pfNumber", "NA");
            String uanNo = (String) empDetails.getOrDefault("uanNumber", "NA");
            String bankName = (String) empDetails.getOrDefault("bankName", "NA");
            String bankBranch = (String) empDetails.getOrDefault("bankBranch", "NA");
            String bankAccountNo = (String) empDetails.getOrDefault("bankAccountNumber", "NA");

            LocalDate dateOfJoining = null;
            if (empDetails.get("dateOfJoining") != null) {
                dateOfJoining = LocalDate.parse(empDetails.get("dateOfJoining").toString());
            }
            if (dateOfJoining != null) {
                LocalDate today = LocalDate.now();
                long daysWorked = ChronoUnit.DAYS.between(dateOfJoining, today);

                if (daysWorked < 30) {
                    throw new PayrollException(
                            "Payslip not available. Employee must complete 1 month of joining. " +
                                    "Current days completed: " + daysWorked
                    );
                }
            }

            // 2️⃣ Fetch or create Annual Salary Structure
            String financialYear = getFinancialYearForMonth(month, year);
            AnnualSalaryStructure annualStructure = annualStructureRepository
                    .findByEmployeeIdAndFinancialYear(employeeId, financialYear)
                    .orElseGet(() -> toEntity(createAnnualSalaryStructure(employeeId, authHeader)));

            // 3️⃣ Fetch attendance summary
            Map<String, Object> mapping = mappingServiceClient.getByPayrollUuid(employeeId.toString());
            String attendanceEmpId = mapping != null && mapping.get("attendanceEmpId") != null
                    ? mapping.get("attendanceEmpId").toString()
                    : employeeId.toString();

            String monthStr = String.format("%d-%02d", year, month);
            Map<String, Object> summary = attendanceClient.getMonthlySummaryForEmployeeStringId(attendanceEmpId, monthStr);

            int actualWorkingDays = ((Number) summary.getOrDefault("actualWorkingDays", 22)).intValue();
            int presentDays = ((Number) summary.getOrDefault("presentDays", 0)).intValue();
            int leaveDays = ((Number) summary.getOrDefault("leaveDays", 0)).intValue();
            double totalWorkHours = Double.parseDouble(summary.getOrDefault("totalWorkHours", "0.0").toString());

            int paidLeaves = Math.min(leaveDays, 2);
            int unpaidLeaves = Math.max(0, leaveDays - 2);

            // 4️⃣ Attendance ratio
            double expectedHours = actualWorkingDays * 8.0;
            double paidLeaveHours = paidLeaves * 8.0;
            double unpaidLeaveHours = unpaidLeaves * 8.0;
            double effectiveWorkedHours = totalWorkHours + paidLeaveHours;
            double payableHours = expectedHours - unpaidLeaveHours;
            double attendanceRatio = payableHours > 0 ? (effectiveWorkedHours / payableHours) : 0.0;
            attendanceRatio = Math.min(attendanceRatio, 1.0);

            // 5️⃣ Salary calculations
            double actualBasic = annualStructure.getMonthlyBasic() * attendanceRatio;
            double actualHra = annualStructure.getMonthlyHra() * attendanceRatio;
            double actualSpecial = annualStructure.getMonthlyAllowances() * attendanceRatio;
            double grossSalary = actualBasic + actualHra + actualSpecial;

            double employeePf = actualBasic * 0.12;
            double professionalTax = 200.0;
            double totalDeductions = employeePf + professionalTax;
            double netSalary = Math.max(grossSalary - totalDeductions, 0);

            // 6️⃣ Build Payslip dynamically
            Payslip payslip = Payslip.builder()
                    .employeeId(employeeId)
                    .employeeName(employeeName)
                    .designation(designation)
                    .department(department)
                    .grade(grade)
                    .vendorCode(vendorCode)
                    .dateOfJoining(dateOfJoining)
                    .pfNo(pfNo)
                    .uanNo(uanNo)
                    .panNo(panNo)
                    .bankName(bankName)
                    .bankBranch(bankBranch)
                    .bankAccountNo(bankAccountNo)
                    .presentDays(presentDays)
                    .holiday(0)
                    .paidLeave(paidLeaves)
                    .unpaidLeave(unpaidLeaves)
                    .gatePass(0)
                    .weekOff(0)
                    .totalSalaryDays(actualWorkingDays)
                    .outDuty(0)
                    .totalWorkHours(totalWorkHours)
                    .basicSalary(actualBasic)
                    .houseRentAllowance(actualHra)
                    .specialAllowance(actualSpecial)
                    .totalEarnings(grossSalary)
                    .employeePf(employeePf)
                    .professionalTax(professionalTax)
                    .totalDeductions(totalDeductions)
                    .employeeNetPay(netSalary)
                    .month(YearMonth.of(year, month).atEndOfMonth())
                    .payslipMonth(month)
                    .payslipYear(year)

                    .status("Processed")
                    .calculationMode("Hourly")
                    .build();

            payslipRepository.save(payslip);
            return MapperUtil.toDTO(payslip);

        } catch (Exception e) {
            e.printStackTrace();
            throw new PayrollException("Error generating payslip: " + e.getMessage());
        }
    }



    // ==================== EXISTING METHODS (No changes) ====================

    @Override
    public List<PayslipDTO> getAllPayslips() {
        return payslipRepository.findAll().stream()
                .map(MapperUtil::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PayrollSummaryDTO getPayrollSummary(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        List<Payslip> payslips = payslipRepository.findByMonth(ym.atEndOfMonth());

        double totalPayroll = payslips.stream().mapToDouble(Payslip::getNetSalary).sum();
        double averageSalary = payslips.isEmpty() ? 0 : totalPayroll / payslips.size();
        long processed = payslips.stream().filter(p -> "Processed".equalsIgnoreCase(p.getStatus())).count();
        long pending = payslips.stream().filter(p -> "Draft".equalsIgnoreCase(p.getStatus())).count();

        return PayrollSummaryDTO.builder()
                .totalPayroll(totalPayroll)
                .averageSalary(averageSalary)
                .processedPayslips(processed)
                .pendingPayslips(pending)
                .build();
    }

    // ==================== PDF GENERATION METHODS ====================

    @Override
    public byte[] generatePayslipPdf(Long payslipId) throws DocumentException {
        try {
            Payslip payslip = payslipRepository.findById(payslipId)
                    .orElseThrow(() -> new PayrollException("Payslip not found with id: " + payslipId));

            // -----------------------------
            // UNIVERSAL MONTH/YEAR FIX
            // -----------------------------
            Integer year = payslip.getPayslipYear();
            Integer month = payslip.getPayslipMonth();

            // 1️⃣ If DB month-year is valid, use it
            if (month != null && month >= 1 && month <= 12 && year != null && year > 1900) {
                payslip.setMonth(LocalDate.of(year, month, 1));
            }

            // 2️⃣ If payslip.getMonth() is present, use it to fix missing values
            else if (payslip.getMonth() != null) {
                LocalDate m = payslip.getMonth();
                payslip.setPayslipYear(m.getYear());
                payslip.setPayslipMonth(m.getMonthValue());
            }

            // 3️⃣ If everything is missing → fallback to current month
            else {
                LocalDate now = LocalDate.now();
                payslip.setMonth(LocalDate.of(now.getYear(), now.getMonthValue(), 1));
                payslip.setPayslipYear(now.getYear());
                payslip.setPayslipMonth(now.getMonthValue());
            }

            // Convert to DTO
            PayslipDTO payslipDTO = MapperUtil.toDTO(payslip);

            // Generate PDF
            return pdfGenerationService.generatePayslipPdf(payslipDTO);

        } catch (Exception e) {
            throw new PayrollException("Error generating PDF for payslip: " + e.getMessage());
        }
    }


    @Override
    public byte[] generatePayslipPdf(UUID employeeId, int month, int year) throws DocumentException {
        // fetch payslip details
        PayslipDTO payslip = generatePayslip(employeeId, month, year, null);

        // call your PdfGenerationService or iText logic
        return pdfGenerationService.generatePayslipPdf(payslip);
    }



    // ==================== HELPER METHODS ====================

    /**
     * Calculate professional tax - Fixed ₹200 as per your example
     */
    private double calculateProfessionalTax(double grossSalary) {
        return 200.0; // Fixed ₹200 as per your calculation example
    }

    /**
     * Get or create attendance mapping for employee
     */
    private String getOrCreateAttendanceMapping(UUID employeeId, String employeeName) {
        Map<String, Object> mapping = mappingServiceClient.getByPayrollUuid(employeeId.toString());
        String attendanceEmpId = null;

        if (mapping != null && mapping.get("attendanceEmpId") != null) {
            attendanceEmpId = mapping.get("attendanceEmpId").toString();
        } else {
            attendanceEmpId = attendanceClient.findAttendanceIdByName(employeeName);
            if (attendanceEmpId != null) {
                mappingServiceClient.createMapping(
                        employeeName,
                        employeeId.toString(),
                        employeeId.toString(),
                        attendanceEmpId,
                        employeeId.toString()
                );
            } else {
                throw new PayrollException("Attendance ID not found for employee: " + employeeName);
            }
        }
        return attendanceEmpId;
    }

    /**
     * Get salary calculation details for a payslip
     */
    public Map<String, Object> getSalaryCalculationDetails(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new PayrollException("Payslip not found"));

        AnnualSalaryStructure annualStructure = annualStructureRepository
                .findByEmployeeId(payslip.getEmployeeId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new PayrollException("Annual structure not found"));

        Map<String, Object> calculationDetails = new HashMap<>();

        // Annual Components
        calculationDetails.put("annualGross", annualStructure.getAnnualGross());
        calculationDetails.put("annualBasic", annualStructure.getAnnualBasic());
        calculationDetails.put("annualHra", annualStructure.getAnnualHra());
        calculationDetails.put("annualAllowances", annualStructure.getAnnualAllowances());
        calculationDetails.put("annualNet", annualStructure.getAnnualNet()); // 🔴 NEW

        // Fixed Monthly Components
        calculationDetails.put("fixedMonthlyGross", annualStructure.getMonthlyGross());
        calculationDetails.put("fixedMonthlyBasic", annualStructure.getMonthlyBasic());
        calculationDetails.put("fixedMonthlyHra", annualStructure.getMonthlyHra());
        calculationDetails.put("fixedMonthlyAllowances", annualStructure.getMonthlyAllowances());
        calculationDetails.put("fixedMonthlyNet", annualStructure.getMonthlyNet()); // 🔴 NEW

        // Actual Paid Components
        calculationDetails.put("actualBasic", payslip.getBasic());
        calculationDetails.put("actualHra", payslip.getHra());
        calculationDetails.put("actualAllowances", payslip.getAllowances());
        calculationDetails.put("actualGross", payslip.getGrossSalary());
        calculationDetails.put("netSalary", payslip.getNetSalary());

        // Calculation Ratios
        double attendanceRatio = payslip.getGrossSalary() / annualStructure.getMonthlyGross();
        calculationDetails.put("attendanceRatio", String.format("%.2f", attendanceRatio * 100) + "%");
        calculationDetails.put("deductionsPercentage",
                String.format("%.2f", (payslip.getDeductions() / payslip.getGrossSalary()) * 100) + "%");

        return calculationDetails;
    }

    // ==================== EXISTING HELPER METHODS ====================

    private String getCurrentFinancialYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        if (now.getMonthValue() >= 4) { // April to March
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    private String getFinancialYearForMonth(int month, int year) {
        if (month >= 4) { // April to March
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    private AnnualSalaryStructureDTO convertToAnnualDTO(AnnualSalaryStructure structure) {
        return AnnualSalaryStructureDTO.builder()
                .id(structure.getId())
                .employeeId(structure.getEmployeeId())
                .employeeName(structure.getEmployeeName())
                .designation(structure.getDesignation())
                .annualBasic(structure.getAnnualBasic())
                .annualHra(structure.getAnnualHra())
                .annualAllowances(structure.getAnnualAllowances())
                .annualGross(structure.getAnnualGross())
                .annualNet(structure.getAnnualNet()) // 🔴 NEW
                .monthlyBasic(structure.getMonthlyBasic())
                .monthlyHra(structure.getMonthlyHra())
                .monthlyAllowances(structure.getMonthlyAllowances())
                .monthlyGross(structure.getMonthlyGross())
                .monthlyNet(structure.getMonthlyNet()) // 🔴 NEW
                .financialYear(structure.getFinancialYear())
                .createdAt(structure.getCreatedAt())
                .status(structure.getStatus())
                .offerLetterId(structure.getOfferLetterId())
                .addedToOfferLetter(structure.isAddedToOfferLetter())
                .build();
    }
    private AnnualSalaryStructure toEntity(AnnualSalaryStructureDTO dto) {
        return AnnualSalaryStructure.builder()
                .id(dto.getId())
                .employeeId(dto.getEmployeeId())
                .employeeName(dto.getEmployeeName())
                .designation(dto.getDesignation())
                .annualBasic(dto.getAnnualBasic())
                .annualHra(dto.getAnnualHra())
                .annualAllowances(dto.getAnnualAllowances())
                .annualGross(dto.getAnnualGross())
                .annualNet(dto.getAnnualNet())
                .monthlyBasic(dto.getMonthlyBasic())
                .monthlyHra(dto.getMonthlyHra())
                .monthlyAllowances(dto.getMonthlyAllowances())
                .monthlyGross(dto.getMonthlyGross())
                .monthlyNet(dto.getMonthlyNet())
                .financialYear(dto.getFinancialYear())
                .status(dto.getStatus())
                .addedToOfferLetter(dto.isAddedToOfferLetter())
                .offerLetterId(dto.getOfferLetterId())
                .build();
    }

    @Override
    public double calculateTotalPayroll(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        List<Payslip> payslips = payslipRepository.findByMonth(ym.atEndOfMonth());

        if (payslips.isEmpty()) {
            System.out.println("⚠️ No payslips found for " + month + "/" + year);
            return 0.0;
        }

        double totalPayroll = payslips.stream()
                .mapToDouble(Payslip::getNetSalary)
                .sum();

        System.out.println("💰 Total Payroll for " + month + "/" + year + " = ₹" + totalPayroll);
        return totalPayroll;
    }
    @Override
    public long countAllPayslips() {
        return payslipRepository.count();
    }
    @Override
    public long countPayslipsByMonth(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        return payslipRepository.findByMonth(ym.atEndOfMonth()).size();
    }

}