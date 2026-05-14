package com.configserver.hrm.payrollService.service.impl;

import com.configserver.hrm.payrollService.dto.OfferLetterDTO;
import com.configserver.hrm.payrollService.service.OfferLetterService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OfferLetterServiceImpl implements OfferLetterService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String EMPLOYEE_API = "http://localhost:8088/api/employees/";
    private static final String PAYROLL_API = "http://localhost:8089/api/payroll/annual-structure";

    private static final String BASIC_USERNAME = "ruchissonawane30@gmail.com";
    private static final String BASIC_PASSWORD = "Ruchi@123";

    // Changed all fonts from HELVETICA to TIMES_ROMAN
    private static final Font FONT_TITLE = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
    private static final Font FONT_SUBTITLE = new Font(Font.FontFamily.TIMES_ROMAN, 11, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL);
    private static final Font FONT_SMALL = new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL);
    private static final Font FONT_TABLE_HEADER = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.BOLD);
    private static final Font FONT_NORMAL_BOLD = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.BOLD);
    private static final BaseColor BLUE_COLOR = new BaseColor(0, 0, 255); // Blue color

    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public byte[] generateOfferLetter(String employeeId) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(BASIC_USERNAME, BASIC_PASSWORD);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<?, ?> empData = getJson(EMPLOYEE_API + employeeId, entity);

        String financialYear = getCurrentFinancialYear();
        String salaryUrl = PAYROLL_API + "?employeeId=" + employeeId + "&financialYear=" + financialYear;
        Map<?, ?> salData = getJson(salaryUrl, entity);

        OfferLetterDTO dto = new OfferLetterDTO();
        dto.setEmployeeId(employeeId);
        dto.setName(asString(empData.get("name")));
        dto.setDesignation(asString(empData.get("designation")));
        dto.setAddress(asString(empData.get("address")));

        String doj = asString(empData.get("dateOfJoining"));
        if (doj != null && !doj.isBlank()) dto.setDateOfJoining(LocalDate.parse(doj));
        dto.setIssueDate(LocalDate.now());
        dto.setReferenceNumber(LocalDate.now().format(REF_DATE) + "-O-L-" + employeeId.substring(0, 4).toUpperCase());

        // Map salary components exactly as per your template
        dto.setAnnualBasic(getDouble(salData.get("annualBasic")));
        dto.setAnnualHra(getDouble(salData.get("annualHra")));
        dto.setAnnualEducationAllowance(getDouble(salData.get("annualEducationAllowance")));
        dto.setAnnualPersonalAllowance(getDouble(salData.get("annualPersonalAllowance")));
        dto.setAnnualSpecialAllowance(getDouble(salData.get("annualSpecialAllowance")));
        dto.setAnnualPf(getDouble(salData.get("annualPf")));
        dto.setAnnualGross(getDouble(salData.get("annualGross")));

        dto.setMonthlyBasic(getDouble(salData.get("monthlyBasic")));
        dto.setMonthlyHra(getDouble(salData.get("monthlyHra")));
        dto.setMonthlyEducationAllowance(getDouble(salData.get("monthlyEducationAllowance")));
        dto.setMonthlyPersonalAllowance(getDouble(salData.get("monthlyPersonalAllowance")));
        dto.setMonthlySpecialAllowance(getDouble(salData.get("monthlySpecialAllowance")));
        dto.setMonthlyPf(getDouble(salData.get("monthlyPf")));
        dto.setMonthlyGross(getDouble(salData.get("monthlyGross")));

        dto.setTotalCTC(dto.getAnnualGross());

        return generatePdf(dto);
    }

    private Map<?, ?> getJson(String url, HttpEntity<Void> entity) {
        try {
            ResponseEntity<?> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null)
                throw new RuntimeException("Failed call: " + url + " status=" + resp.getStatusCode());
            return (LinkedHashMap<?, ?>) resp.getBody();
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new RuntimeException("401 Unauthorized calling " + url + " — check BasicAuth credentials", ex);
        }
    }

    private String getCurrentFinancialYear() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int startYear;
        int endYear;
        if (today.getMonthValue() >= 4) {
            startYear = year;
            endYear = year + 1;
        } else {
            startYear = year - 1;
            endYear = year;
        }
        return startYear + "-" + endYear;
    }

    private String asString(Object o) { return o == null ? "" : o.toString(); }

    private double getDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private byte[] generatePdf(OfferLetterDTO dto) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Rectangle pageSize = PageSize.A4;
        Document doc = new Document(pageSize, 36, 36, 130, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, baos);

        HeaderFooter event = new HeaderFooter();
        writer.setPageEvent(event);

        doc.open();

        // Date and Reference table (right aligned) - EXACT FORMAT
        PdfPTable meta = new PdfPTable(2);
        meta.setWidths(new float[]{3f, 3f});
        meta.setWidthPercentage(100);
        meta.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Date cell - left aligned
        PdfPCell dateCell = new PdfPCell(new Phrase("Date: " + dto.getIssueDate().format(DISPLAY_DATE), FONT_NORMAL));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        meta.addCell(dateCell);

        // Reference cell - right aligned
        PdfPCell refCell = new PdfPCell(new Phrase("Ref: " + dto.getReferenceNumber(), FONT_NORMAL));
        refCell.setBorder(Rectangle.NO_BORDER);
        refCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        meta.addCell(refCell);

        meta.setSpacingAfter(15f);
        doc.add(meta);

        doc.add(Chunk.NEWLINE);

        // To address - EXACT FORMAT
        // To address - ALTERNATIVE (SAME LINE)
        Paragraph to = new Paragraph();
        to.add(new Phrase("To,", FONT_NORMAL));
        to.add(Chunk.NEWLINE);
        to.add(new Phrase(dto.getName(), FONT_NORMAL));
        to.add(Chunk.NEWLINE);
        to.add(new Phrase(dto.getAddress() + "Pune, Maharashtra", FONT_NORMAL));
        to.add(Chunk.NEWLINE);
        doc.add(to);

        // Subject - EXACT FORMAT
        // Changed font to Times Roman
        Paragraph subject = new Paragraph("Subject: Offer of Employment – " + dto.getDesignation(), new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL, BLUE_COLOR));
        subject.setSpacingBefore(10f);
        subject.setSpacingAfter(10f);
        doc.add(subject);

        // Opening paragraph - EXACT FORMAT
        Paragraph opening = new Paragraph(
                "Further to your interview, we are pleased to offer you the position of " + dto.getDesignation() +
                        " with effect from " + (dto.getDateOfJoining() != null ? dto.getDateOfJoining().format(DISPLAY_DATE) : "") +
                        ", on the terms and conditions set forth below.", FONT_NORMAL);
        opening.setSpacingAfter(10f);
        doc.add(opening);

        // Documents Required - EXACT FORMAT (FIXED)
        // Changed font to Times Roman
        Paragraph docsHeader = new Paragraph("Documents Required at Joining:", new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL, BLUE_COLOR));
        docsHeader.setSpacingAfter(5f);
        doc.add(docsHeader);

        com.itextpdf.text.List docs = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
        docs.setListSymbol("\u2022         ");
        docs.setIndentationLeft(20);
        docs.add(new ListItem("Four passport size photographs", FONT_NORMAL));
        docs.add(new ListItem("Copies of all educational degrees, mark sheets & training certificates", FONT_NORMAL));
        docs.add(new ListItem("Experience letters from all previous employers", FONT_NORMAL));
        docs.add(new ListItem("Latest salary slips from current employer", FONT_NORMAL));
        docs.add(new ListItem("PAN Card & Aadhaar Card", FONT_NORMAL));
        doc.add(docs);

        // Add spacing after the list
        doc.add(Chunk.NEWLINE);

        // Terms and Conditions - EXACT FORMAT
        // Changed font to Times Roman
        Paragraph termsHeader = new Paragraph("Terms and Conditions", new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL, BLUE_COLOR));
        termsHeader.setSpacingAfter(5f);
        doc.add(termsHeader);

        // Add all terms exactly as in your template
        addTerm(doc, 1, "Probation Period:",
                "You will be on probation for six (6) months. Upon satisfactory performance, you will be confirmed in writing. In the absence of such confirmation, probation will be deemed extended. During probation, leave entitlement is not applicable unless approved under exceptional circumstances.");

        addTerm(doc, 2, "Leave:",
                "Upon confirmation, you will be entitled to twelve (12) days of privilege leave annually.");

        addTerm(doc, 3, "Service Rules & Conduct:",
                "You shall comply with all company policies. Without prior written approval, you may not engage in any other employment or business. Violation may lead to termination.");

        addTerm(doc, 4, "Confidentiality:",
                "You shall not, during or after employment, disclose any confidential information without prior written consent.");

        addTerm(doc, 5, "Intellectual Property:",
                "Any invention, design, or improvement developed during employment shall be the property of the company.");

        addTerm(doc, 6, "Termination:",
                "Either party may terminate employment with one (1) month's written notice or salary in lieu thereof. The company reserves the right to terminate without notice in cases of misconduct, subject to applicable laws.");

        addTerm(doc, 7, "Resignation:",
                "You must serve a thirty (30) day notice period upon resignation.");

        addTerm(doc, 8, "Performance Review:",
                "Performance will be reviewed on attendance, work quality, adherence to policies, and confidentiality compliance. Increments are at management's discretion.");

        addTerm(doc, 9, "Performance Improvement Plan (PIP):",
                "If required, a PIP will be initiated after the initial three (3) month training period.");

        // Check if we need a new page for salary section
        if (writer.getVerticalPosition(false) < 300) {
            doc.newPage();
        }

        // Salary Break-Up section - EXACT FORMAT
        Paragraph salaryHeader = new Paragraph("10. Salary Break-Up", FONT_NORMAL_BOLD);
        salaryHeader.setSpacingAfter(5f);
        doc.add(salaryHeader);

        Paragraph fixedComp = new Paragraph("Fixed Compensation", FONT_NORMAL);
        fixedComp.setSpacingAfter(5f);
        doc.add(fixedComp);

        // Salary table - EXACT FORMAT as per PDF
        PdfPTable salaryTable = new PdfPTable(3);
        salaryTable.setWidthPercentage(100);
        salaryTable.setWidths(new float[]{3f, 2f, 2f});

        // Table headers
        addTableHeader(salaryTable, "Component");
        addTableHeader(salaryTable, "Amount (\u20B9)");
        addTableHeader(salaryTable, "Monthly (\u20B9)");

        // Table rows
        addTableRow(salaryTable, "Basic Salary (50%)", dto.getAnnualBasic(), dto.getMonthlyBasic());
        addTableRow(salaryTable, "House Rent Allowance", dto.getAnnualHra(), dto.getMonthlyHra());
        addTableRow(salaryTable, "Education Allowance", dto.getAnnualEducationAllowance(), dto.getMonthlyEducationAllowance());
        addTableRow(salaryTable, "Personal Allowance", dto.getAnnualPersonalAllowance(), dto.getMonthlyPersonalAllowance());
        addTableRow(salaryTable, "Special Allowance", dto.getAnnualSpecialAllowance(), dto.getMonthlySpecialAllowance());
        addTableRow(salaryTable, "Provident Fund (fixed)", dto.getAnnualPf(), dto.getMonthlyPf());

        // Summary rows with proper formatting
        addSummaryRow(salaryTable, "Fixed Monthly Gross", 0, dto.getMonthlyGross());
        addSummaryRow(salaryTable, "Fixed Annual Gross", dto.getAnnualGross(), 0);

        doc.add(salaryTable);
        doc.add(Chunk.NEWLINE);

        // CTC Summary - EXACT FORMAT
        Paragraph ctcHeader = new Paragraph("Overall CTC Summary", FONT_NORMAL_BOLD);
        ctcHeader.setSpacingAfter(5f);
        doc.add(ctcHeader);

        PdfPTable ctc = new PdfPTable(2);
        ctc.setWidthPercentage(50);
        ctc.setHorizontalAlignment(Element.ALIGN_LEFT);
        ctc.setWidths(new float[]{3f, 2f});

        addCell(ctc, "Head", FONT_NORMAL_BOLD);
        addCell(ctc, "Amount (\u20B9)", FONT_NORMAL_BOLD);
        addCell(ctc, "Gross Fixed Compensation", FONT_NORMAL);
        addCell(ctc, DF.format(dto.getAnnualGross()), FONT_NORMAL);
        addCell(ctc, "Total Cost to Company (CTC)", FONT_NORMAL);
        addCell(ctc, DF.format(dto.getTotalCTC()), FONT_NORMAL);

        doc.add(ctc);

        // UPDATED ACCEPTANCE SECTION WITH DATE ON RIGHT SIDE
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
        Paragraph acceptText = new Paragraph("Please sign and return a copy of this letter to confirm your acceptance of the above terms and conditions.", FONT_NORMAL);
        acceptText.setSpacingAfter(10f);
        doc.add(acceptText);

        doc.add(new Paragraph("Name: …………………………………………", FONT_NORMAL));

        // Create table for signature and date with proper alignment
        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100);
        signatureTable.setWidths(new float[]{3f, 2f});
        signatureTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Signature cell (left aligned)
        PdfPCell signatureCell = new PdfPCell(new Phrase("Signature: …………………………………", FONT_NORMAL));
        signatureCell.setBorder(Rectangle.NO_BORDER);
        signatureCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        signatureTable.addCell(signatureCell);

        // Date cell (right aligned)
        PdfPCell dateCellAccept = new PdfPCell(new Phrase("Date: ___/___/_____", FONT_NORMAL));
        dateCellAccept.setBorder(Rectangle.NO_BORDER);
        dateCellAccept.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signatureTable.addCell(dateCellAccept);

        doc.add(signatureTable);
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        // Company footer - EXACT FORMAT
        Paragraph companyFooter = new Paragraph("CONFIG SERVER LLP", FONT_NORMAL_BOLD);
        companyFooter.setAlignment(Element.ALIGN_CENTER);
        doc.add(companyFooter);

        Paragraph tagline = new Paragraph("Innovate Integrate Elevate", FONT_NORMAL);
        tagline.setAlignment(Element.ALIGN_CENTER);
        doc.add(tagline);

        doc.close();
        return baos.toByteArray();
    }

    private void addTerm(Document doc, int no, String title, String text) throws DocumentException {
        // Number and title in bold (original black color)
        Paragraph titlePara = new Paragraph();
        titlePara.add(new Phrase(no + ". " + title, FONT_NORMAL_BOLD));
        titlePara.setSpacingAfter(4f); // Small space after title
        doc.add(titlePara);

        // Description text in normal (original black color)
        Paragraph descPara = new Paragraph(text, FONT_NORMAL);

        // ADD EXTRA SPACE AFTER POINT 6 (TERMINATION)
        if (no == 6) {
            descPara.setSpacingAfter(80f); // Extra space after Termination point
        } else {
            descPara.setSpacingAfter(12f); // Normal space for other points
        }

        descPara.setFirstLineIndent(0);
        doc.add(descPara);
    }

    private void addTableHeader(PdfPTable t, String s) {
        PdfPCell c = new PdfPCell(new Phrase(s, FONT_TABLE_HEADER));
        c.setBackgroundColor(new BaseColor(240, 240, 240)); // Light gray background
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(5);
        t.addCell(c);
    }

    private void addTableRow(PdfPTable t, String label, double annual, double monthly) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, FONT_NORMAL));
        PdfPCell c2 = new PdfPCell(new Phrase(annual > 0 ? DF.format(annual) : "—", FONT_NORMAL));
        PdfPCell c3 = new PdfPCell(new Phrase(monthly > 0 ? DF.format(monthly) : "—", FONT_NORMAL));

        c1.setPadding(5);
        c2.setPadding(5);
        c3.setPadding(5);

        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);

        t.addCell(c1);
        t.addCell(c2);
        t.addCell(c3);
    }

    private void addSummaryRow(PdfPTable t, String label, double annual, double monthly) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, FONT_NORMAL_BOLD));
        PdfPCell c2 = new PdfPCell(new Phrase(annual > 0 ? DF.format(annual) : "—", FONT_NORMAL_BOLD));
        PdfPCell c3 = new PdfPCell(new Phrase(monthly > 0 ? DF.format(monthly) : "—", FONT_NORMAL_BOLD));

        c1.setPadding(5);
        c2.setPadding(5);
        c3.setPadding(5);

        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);

        // Add border top for summary rows
        c1.setBorderWidthTop(1);
        c2.setBorderWidthTop(1);
        c3.setBorderWidthTop(1);
        c1.setBorderColorTop(BaseColor.BLACK);
        c2.setBorderColorTop(BaseColor.BLACK);
        c3.setBorderColorTop(BaseColor.BLACK);

        t.addCell(c1);
        t.addCell(c2);
        t.addCell(c3);
    }

    private void addCell(PdfPTable t, String s, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(s, font));
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.addCell(c);
    }

    private class HeaderFooter extends PdfPageEventHelper {
        private Image logo;

        public HeaderFooter() {
            try (InputStream is = getClass().getResourceAsStream("/static/logo.png")) {
                if (is != null) {
                    byte[] arr = IOUtils.toByteArray(is);
                    logo = Image.getInstance(arr);
                    logo.scaleToFit(200, 150);
                }
            } catch (Exception e) {
                // Logo not found, continue without it
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                // HEADER - ON EVERY PAGE
                PdfPTable header = new PdfPTable(1);
                header.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());

                PdfPCell cell = new PdfPCell();
                cell.setBorder(Rectangle.NO_BORDER);

                PdfPTable inner = new PdfPTable(2);
                inner.setWidths(new float[]{1f, 3f});
                inner.setWidthPercentage(100);

                // Logo cell
                PdfPCell logoCell = new PdfPCell();
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                if (logo != null) {
                    logoCell.addElement(logo);
                } else {
                    logoCell.addElement(new Phrase("")); // Empty if no logo
                }
                inner.addCell(logoCell);

                // Company info cell - right aligned (EXACT FORMAT)
                // Changed fonts to Times Roman
                PdfPCell infoCell = new PdfPCell();
                infoCell.setBorder(Rectangle.NO_BORDER);
                infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

                Paragraph companyInfo = new Paragraph();
                companyInfo.add(new Phrase("Office No. 303, A Wing, Laxmi Horizon, HDFC\n", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL)));
                companyInfo.add(new Phrase("Bank Punawale, Pune – 411033\n", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL)));
                companyInfo.add(new Phrase("Tel. 020 47211265\n", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL)));
                companyInfo.add(new Phrase("www.configserverllp.com\n", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BLUE_COLOR)));
                companyInfo.add(new Phrase("info@configserverllp.com", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BLUE_COLOR)));
                companyInfo.setAlignment(Element.ALIGN_RIGHT);

                infoCell.addElement(companyInfo);
                inner.addCell(infoCell);

                cell.addElement(inner);
                header.addCell(cell);

                // Position header at top
                header.writeSelectedRows(0, -1, document.leftMargin(),
                        document.getPageSize().getHeight() - 20, writer.getDirectContent());

                // ADD HORIZONTAL LINE RIGHT AFTER HEADER - ON EVERY PAGE
                PdfContentByte headerCanvas = writer.getDirectContent();
                headerCanvas.saveState();
                headerCanvas.setLineWidth(0.5f); // Thin line
                headerCanvas.moveTo(document.leftMargin(), document.getPageSize().getHeight() - 120);
                headerCanvas.lineTo(document.getPageSize().getWidth() - document.rightMargin(), document.getPageSize().getHeight() - 120);
                headerCanvas.stroke();
                headerCanvas.restoreState();

                // FOOTER - ON EVERY PAGE
                // Changed fonts to Times Roman
                PdfContentByte footerCanvas = writer.getDirectContent();

                // Draw horizontal line above footer
                footerCanvas.saveState();
                footerCanvas.setLineWidth(0.5f);
                footerCanvas.moveTo(document.leftMargin(), document.bottomMargin() + 20);
                footerCanvas.lineTo(document.getPageSize().getWidth() - document.rightMargin(), document.bottomMargin() + 20);
                footerCanvas.stroke();
                footerCanvas.restoreState();

                // Footer table
                PdfPTable footer = new PdfPTable(1);
                footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());

                PdfPCell footerCell = new PdfPCell();
                footerCell.setBorder(Rectangle.NO_BORDER);
                footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                footerCell.setPaddingTop(5); // Add some padding after the line

                Paragraph footerInfo = new Paragraph();
                footerInfo.add(new Phrase("Config Server LLP", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL)));
                footerInfo.add(Chunk.NEWLINE);
                footerInfo.add(new Phrase("Office No. 303, 313, A Wing, Laxmi Horizon, HDFC Bank, Punawale, Pune – 411033, Maharashtra, India | ", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL)));
                footerInfo.add(new Phrase("LLP No. ACF-7649 | ", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL)));
                footerInfo.add(new Phrase("www.configserverllp.com | ", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BLUE_COLOR)));
                footerInfo.add(new Phrase("Tel. 020 47211265 | ", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL)));
                footerInfo.add(new Phrase("info@configserverllp.com", new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.NORMAL, BLUE_COLOR)));
                footerInfo.setAlignment(Element.ALIGN_CENTER);

                footerCell.addElement(footerInfo);
                footer.addCell(footerCell);

                // Position footer at bottom
                footer.writeSelectedRows(0, -1, document.leftMargin(),
                        document.bottomMargin() + 20, writer.getDirectContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}