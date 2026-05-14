package com.configserver.hrm.payrollService.service;

import com.configserver.hrm.payrollService.dto.PayslipDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private static final Font FONT_HEADER = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font FONT_SUBHEADER = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font FONT_BODY = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font FONT_BODY_BOLD = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font FONT_FOOTER = new Font(Font.FontFamily.HELVETICA, 8);
    private static final Font FONT_FOOTER_BOLD = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
    private static final Font FONT_LINK = new Font(Font.FontFamily.HELVETICA, 8, Font.UNDERLINE, new BaseColor(0, 0, 255));

    private PdfPCell cell(String text, Font font, int border, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "-", font));
        c.setHorizontalAlignment(align);
        c.setPadding(4f);
        c.setBorder(border);
        return c;
    }

    private PdfPCell infoCell(String text, Font font, int border, int align, float padding) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "-", font));
        c.setHorizontalAlignment(align);
        c.setPadding(padding);
        c.setBorder(border);
        return c;
    }

    private String formatDouble(Double value) {
        if (value == null) value = 0.0;
        return new DecimalFormat("#,##0.00").format(value);
    }

    private String numberToWords(Double num) {
        if (num == null) num = 0.0;
        long integerPart = Math.round(num);
        if (integerPart == 0) return "Zero";

        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
                "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        StringBuilder words = new StringBuilder();

        if ((integerPart / 10000000) > 0) {
            words.append(numberToWords((double) (integerPart / 10000000))).append(" Crore ");
            integerPart %= 10000000;
        }
        if ((integerPart / 100000) > 0) {
            words.append(numberToWords((double) (integerPart / 100000))).append(" Lakh ");
            integerPart %= 100000;
        }
        if ((integerPart / 1000) > 0) {
            words.append(numberToWords((double) (integerPart / 1000))).append(" Thousand ");
            integerPart %= 1000;
        }
        if ((integerPart / 100) > 0) {
            words.append(numberToWords((double) (integerPart / 100))).append(" Hundred ");
            integerPart %= 100;
        }
        if (integerPart > 0) {
            if ((integerPart / 100) > 0) words.append(" and ");
            if (integerPart < 20) words.append(units[(int) integerPart]);
            else {
                words.append(tens[(int) integerPart / 10]);
                if ((integerPart % 10) > 0)
                    words.append(" ").append(units[(int) integerPart % 10]);
            }
        }
        return words.toString().trim();
    }

    private String getPayslipTitle(PayslipDTO emp) {
        String monthName = java.time.Month.of(emp.getPayslipMonth()).name();
        monthName = monthName.substring(0,1) + monthName.substring(1).toLowerCase(); // Capitalize
        return "Pay Slip For the Month: " + monthName + " " + emp.getPayslipYear();
    }


    public byte[] generatePayslipPdf(PayslipDTO emp) throws DocumentException {

        if (emp == null) throw new IllegalArgumentException("PayslipDTO cannot be null");

        Document doc = new Document(PageSize.A4, 40, 40, 30, 60);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new FooterEvent());
        doc.open();

        /** ================= HEADER ================= **/
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell titleBlock = new PdfPCell();
        titleBlock.setBorder(Rectangle.NO_BORDER);

        try {
            Image logo = Image.getInstance(getClass().getResource("/static/logo.png"));
            logo.scaleToFit(200, 160);
            logo.setAlignment(Image.ALIGN_RIGHT);
            titleBlock.addElement(logo);
        } catch (Exception e) {
            System.err.println("Logo not found: " + e.getMessage());
        }

        Paragraph companyLine1 = new Paragraph("M/S CONFIG SERVER LLP", FONT_HEADER);
        companyLine1.setAlignment(Element.ALIGN_CENTER);
        titleBlock.addElement(companyLine1);

        Paragraph address = new Paragraph("S.NO. 52(P), OFFICE NO 303, LAXMI HORIZON, PUNAWALE, PUNE 411057", FONT_BODY);
        address.setAlignment(Element.ALIGN_CENTER);
        titleBlock.addElement(address);

        Paragraph paySlipTitle = new Paragraph(getPayslipTitle(emp), FONT_SUBHEADER);
        paySlipTitle.setAlignment(Element.ALIGN_CENTER);
        paySlipTitle.setSpacingAfter(6f);
        titleBlock.addElement(paySlipTitle);

        header.addCell(titleBlock);
        doc.add(header);

        /** =========================================================================
         *      REMOVED OUTER BOX - NOW ADDING SECTIONS DIRECTLY TO DOCUMENT
         * ========================================================================= */

        /** ================= COMBINED EMPLOYEE INFO & ATTENDANCE TABLE ================= **/
        PdfPTable combinedTable = new PdfPTable(6);
        combinedTable.setWidthPercentage(100);
        combinedTable.setSpacingBefore(2f);
        combinedTable.setWidths(new float[]{1.7f, 2.3f, 1.7f, 2.3f, 1.7f, 2.3f});

        // Employee Information Section
        combinedTable.addCell(cell("Employee ID", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getEmployeeId() != null ? emp.getEmployeeId().toString() : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Employee Name", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getEmployeeName(), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Branch", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getBankBranch() != null ? emp.getBankBranch() : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Department", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getDepartment(), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Designation", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getDesignation(), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Grade", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getGrade(), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Vendor Code", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getVendorCode() != null ? emp.getVendorCode() : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Date of Joining", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getDateOfJoining() != null ? emp.getDateOfJoining().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Payslip Month", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(String.valueOf(emp.getPayslipMonth()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Payslip Year", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(String.valueOf(emp.getPayslipYear()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("PF No", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getPfNo() != null ? emp.getPfNo() : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Bank A/c No", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getBankAccountNo() != null ? emp.getBankAccountNo() : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Bank Name", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getBankName() != null ? emp.getBankName() : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("PAN No", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(emp.getPanNo() != null ? emp.getPanNo() : "-", FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        // UAN row spanning full width
        PdfPCell uanLabel = new PdfPCell(new Phrase("UAN No", FONT_BODY_BOLD));
        uanLabel.setColspan(3);
        uanLabel.setBorder(Rectangle.BOX);
        uanLabel.setPadding(4f);
        combinedTable.addCell(uanLabel);

        PdfPCell uanValue = new PdfPCell(new Phrase(emp.getUanNo() != null ? emp.getUanNo() : "NA", FONT_BODY));
        uanValue.setColspan(3);
        uanValue.setBorder(Rectangle.BOX);
        uanValue.setPadding(4f);
        combinedTable.addCell(uanValue);

        // Attendance Summary Section - Added as regular rows in the same table
        combinedTable.addCell(cell("Present Day", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getPresentDays()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Holiday", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getHoliday()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Paid Leave", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getPaidLeave()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Week Off", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getWeekOff()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Out Duty", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getOutDuty()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Unpaid Leave", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getUnpaidLeave()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Gate Pass", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getGatePass()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        combinedTable.addCell(cell("Total Sal Day", FONT_BODY_BOLD, Rectangle.BOX, Element.ALIGN_LEFT));
        combinedTable.addCell(cell(safeStr(emp.getTotalSalaryDays()), FONT_BODY, Rectangle.BOX, Element.ALIGN_LEFT));

        doc.add(combinedTable);

        // thin horizontal separator between sections
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell sepCell = new PdfPCell(new Phrase(""));
        sepCell.setBorder(Rectangle.TOP);
        sepCell.setBorderWidthTop(0.5f);
        sepCell.setPadding(6f);
        sep.addCell(sepCell);
        doc.add(sep);

        /** ================= SALARY TABLE (with vertical lines between columns) ================= **/
        PdfPTable sal = new PdfPTable(4);
        sal.setWidthPercentage(100);
        sal.setSpacingBefore(2f);

// Helper: create cells with vertical borders only (LEFT and RIGHT)
        java.util.function.BiFunction<String, Font, PdfPCell> rowCell = (text, font) -> {
            PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "-", font));
            c.setBorder(Rectangle.LEFT | Rectangle.RIGHT); // Only vertical borders
            c.setBorderWidth(0.5f);
            c.setPadding(6f);
            return c;
        };

// Header row - with bottom border and vertical lines
        PdfPCell h1 = new PdfPCell(new Phrase("Particulars", FONT_BODY_BOLD));
        PdfPCell h2 = new PdfPCell(new Phrase("Total Amount", FONT_BODY_BOLD));
        PdfPCell h3 = new PdfPCell(new Phrase("Particulars", FONT_BODY_BOLD));
        PdfPCell h4 = new PdfPCell(new Phrase("Deduction", FONT_BODY_BOLD));

// Set borders for header: bottom + vertical lines
        h1.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        h2.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        h3.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        h4.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);

        h1.setBorderWidthBottom(0.6f);
        h2.setBorderWidthBottom(0.6f);
        h3.setBorderWidthBottom(0.6f);
        h4.setBorderWidthBottom(0.6f);
        h1.setBorderWidth(0.5f);
        h2.setBorderWidth(0.5f);
        h3.setBorderWidth(0.5f);
        h4.setBorderWidth(0.5f);
        h1.setPadding(6f);
        h2.setPadding(6f);
        h3.setPadding(6f);
        h4.setPadding(6f);

        sal.addCell(h1);
        sal.addCell(h2);
        sal.addCell(h3);
        sal.addCell(h4);

// Rows - all regular rows with vertical lines only
        sal.addCell(rowCell.apply("Basic Salary", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getBasicSalary()), FONT_BODY));
        sal.addCell(rowCell.apply("Professional Tax", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getProfessionalTax()), FONT_BODY));

        sal.addCell(rowCell.apply("Extra Days Worked", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getExtraDaysWorked()), FONT_BODY));
        sal.addCell(rowCell.apply("Employee PF", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getPfAmount()), FONT_BODY));

        sal.addCell(rowCell.apply("House Rent Allowance (AMT)", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getHouseRentAllowance()), FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));

        sal.addCell(rowCell.apply("Children Education (AMT)", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getChildrenEducation()), FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));

        sal.addCell(rowCell.apply("Monthly Other B (AMT)", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getMonthlyOtherB()), FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));

        sal.addCell(rowCell.apply("Performance Bonus (AMT)", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getPerformanceBonus()), FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));

// Special Allowance row
        sal.addCell(rowCell.apply("Special Allowance (AMT)", FONT_BODY));
        sal.addCell(rowCell.apply(formatDouble(emp.getSpecialAllowance()), FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));
        sal.addCell(rowCell.apply("", FONT_BODY));

// Totals separator - top border with vertical lines
        PdfPCell totalsSeparator = new PdfPCell(new Phrase(""));
        totalsSeparator.setColspan(4);
        totalsSeparator.setBorder(Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
        totalsSeparator.setBorderWidthTop(0.6f);
        totalsSeparator.setBorderWidth(0.5f);
        totalsSeparator.setPaddingTop(6f);
        sal.addCell(totalsSeparator);

// Totals rows - bottom border with vertical lines
        PdfPCell totalEarningsLabel = new PdfPCell(new Phrase("Total Earnings :", FONT_BODY_BOLD));
        totalEarningsLabel.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        totalEarningsLabel.setBorderWidthBottom(0.6f);
        totalEarningsLabel.setBorderWidth(0.5f);
        totalEarningsLabel.setPadding(6f);
        sal.addCell(totalEarningsLabel);

        PdfPCell totalEarningsValue = new PdfPCell(new Phrase(formatDouble(emp.getTotalEarnings()), FONT_BODY_BOLD));
        totalEarningsValue.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        totalEarningsValue.setBorderWidthBottom(0.6f);
        totalEarningsValue.setBorderWidth(0.5f);
        totalEarningsValue.setPadding(6f);
        sal.addCell(totalEarningsValue);

        PdfPCell totalDeductionsLabel = new PdfPCell(new Phrase("Total Deduction :", FONT_BODY_BOLD));
        totalDeductionsLabel.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        totalDeductionsLabel.setBorderWidthBottom(0.6f);
        totalDeductionsLabel.setBorderWidth(0.5f);
        totalDeductionsLabel.setPadding(6f);
        sal.addCell(totalDeductionsLabel);

        PdfPCell totalDeductionsValue = new PdfPCell(new Phrase(formatDouble(emp.getTotalDeductions()), FONT_BODY_BOLD));
        totalDeductionsValue.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        totalDeductionsValue.setBorderWidthBottom(0.6f);
        totalDeductionsValue.setBorderWidth(0.5f);
        totalDeductionsValue.setPadding(6f);
        sal.addCell(totalDeductionsValue);

// Net Pay row - with borders like other total rows
        PdfPCell netPaySeparator = new PdfPCell(new Phrase(""));
        netPaySeparator.setColspan(4);
        netPaySeparator.setBorder(Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
        netPaySeparator.setBorderWidthTop(0.6f);
        netPaySeparator.setBorderWidth(0.5f);
        netPaySeparator.setPaddingTop(6f);
        sal.addCell(netPaySeparator);

        PdfPCell netPayLabel = new PdfPCell(new Phrase("Employee Net Pay :", FONT_BODY_BOLD));
        netPayLabel.setColspan(2);
        netPayLabel.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        netPayLabel.setBorderWidthBottom(0.6f);
        netPayLabel.setBorderWidth(0.5f);
        netPayLabel.setPadding(6f);
        sal.addCell(netPayLabel);

        PdfPCell netPayValue = new PdfPCell(new Phrase(formatDouble(emp.getEmployeeNetPay()) + " (" + numberToWords(emp.getEmployeeNetPay()) + " Only)", FONT_BODY_BOLD));
        netPayValue.setColspan(2);
        netPayValue.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        netPayValue.setBorderWidthBottom(0.6f);
        netPayValue.setBorderWidth(0.5f);
        netPayValue.setPadding(6f);
        sal.addCell(netPayValue);

        doc.add(sal);

        doc.close();
        return out.toByteArray();
    }

    private static String safeStr(Number n) {
        return n == null ? "-" : String.valueOf(n);
    }

    /** ================= FOOTER ================= **/
    private static class FooterEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable footer = new PdfPTable(1);
                footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                footer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                Phrase phrase = new Phrase();
                phrase.add(new Chunk("Config Server LLP | Registered Office: ", FONT_FOOTER_BOLD));
                phrase.add(new Chunk("Office No. 303, 313, A Wing, Laxmi Horizon, HDFC Bank, Punawale, ", FONT_FOOTER));
                phrase.add(new Chunk("Pune 411033, Maharashtra | LLP No. ACF-7649 | ", FONT_FOOTER));
                Chunk link = new Chunk("www.configserverllp.com", FONT_LINK);
                link.setAnchor("https://www.configserverllp.com");
                phrase.add(link);
                phrase.add(new Chunk(" | +91 9156486909", FONT_FOOTER));

                PdfPCell cell = new PdfPCell(phrase);
                cell.setBorder(Rectangle.TOP);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPaddingTop(4f);
                footer.addCell(cell);

                footer.writeSelectedRows(0, -1, document.leftMargin(), 60, writer.getDirectContent());

            } catch (Exception ignored) {}
        }
    }
}