package com.configserver.hrm.attendanceService.service.impl;

import com.configserver.hrm.attendanceService.client.EmployeeMappingClient;
import com.configserver.hrm.attendanceService.client.LeaveClient;
import com.configserver.hrm.attendanceService.config.EmployeeEmailConfig;
import com.configserver.hrm.attendanceService.dto.AttendanceRequestDTO;
import com.configserver.hrm.attendanceService.dto.AttendanceSummaryDTO;
import com.configserver.hrm.attendanceService.dto.DailySummaryDTO;
import com.configserver.hrm.attendanceService.dto.EmployeeMappingResponse;
import com.configserver.hrm.attendanceService.entity.AttendanceStatus;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import com.configserver.hrm.attendanceService.exception.ExcelDownloadException;
import com.configserver.hrm.attendanceService.exception.InvalidAttendanceDataException;
import com.configserver.hrm.attendanceService.external.EtimeOfficeService;
import com.configserver.hrm.attendanceService.repository.EmployeeAttendanceRepository;
import com.configserver.hrm.attendanceService.service.AttendanceService;
import com.configserver.hrm.attendanceService.service.EmailService;
import com.configserver.hrm.attendanceService.service.PlaywrightMonthlyReportDownloader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private EmployeeAttendanceRepository repository;
    @Autowired
    LeaveClient leaveClient;
    @Autowired
    PlaywrightMonthlyReportDownloader playwrightMonthlyReportDownloader;

    @Autowired
    EmployeeMappingClient employeeMappingClient;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmployeeEmailConfig employeeEmailConfig;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private EtimeOfficeService etimeOfficeService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LeaveClient leaveClients;

    @Override
    public void importAttendance(List<AttendanceRequestDTO> attendanceList) {
        attendanceList.stream()
                .filter(dto -> !repository.existsByEmployeeIdAndDate(dto.getEmployeeId(), dto.getDate()))
                .map(this::mapDtoToEntity)//convert dto to entity
                .forEach(repository::save);// method reference:: // loop over dtos and internally save itin db
    }

    @Override
    @Transactional
    public List<EmployeeAttendance> importDailyAttendanceFromEtimeOffice() {
        LocalDate today = LocalDate.now();
        return importAttendanceFromEtimeOffice(today);
    }


    @Override
    @Transactional
    public List<EmployeeAttendance> importAttendanceFromEtimeOffice(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidAttendanceDataException("Future date not allowed: " + date);
        }

        try {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            byte[] excelData = etimeOfficeService.downloadDailyReport(formattedDate);
            if (excelData == null || excelData.length == 0) {
                throw new ExcelDownloadException("No data from eTimeOffice for date: " + formattedDate);
            }

            // ✅ Overwrite: delete existing records for that date
            repository.deleteByDate(date);

            List<AttendanceRequestDTO> attendanceList = parseExcelToDTO(excelData, date);
            List<EmployeeAttendance> saved = new ArrayList<>();

            for (AttendanceRequestDTO dto : attendanceList) {
                EmployeeAttendance entity = mapDtoToEntity(dto);
                entity.setSourceType("DAILY");
                saved.add(repository.save(entity));
            }

            return saved;
        } catch (Exception e) {
            throw new ExcelDownloadException("Failed to import attendance for date " + date, e);
        }
    }
    @Override
    public List<EmployeeAttendance> getDailyAttendance(LocalDate date) {
        return repository.findByDate(date);
    }

    @Override
    public List<EmployeeAttendance> getEmployeeAttendance(String employeeId, LocalDate from, LocalDate to) {
        return repository.findByEmployeeIdAndDateBetween(employeeId, from, to);
    }

    @Override
    public byte[] downloadAttendanceReport(LocalDate date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String formattedDate = date.format(formatter);

            byte[] excelData = etimeOfficeService.downloadDailyReport(formattedDate);
            if (excelData == null || excelData.length == 0) {
                throw new ExcelDownloadException("No report found for date: " + formattedDate);
            }
            return excelData;
        } catch (Exception e) {
            throw new ExcelDownloadException("Failed to download report for date: " + date, e);
        }
    }

    public byte[] downloadMonthlyReport(String monthYear, List<String> employeeIds) {
        // Simply delegate to ETimeOfficeService
        return etimeOfficeService.downloadMonthlyReport(monthYear, employeeIds);
    }

    private EmployeeAttendance mapDtoToEntity(AttendanceRequestDTO dto) {
        EmployeeAttendance entity = new EmployeeAttendance();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setEmployeeName(dto.getEmployeeName());
        entity.setShift(dto.getShift());
        entity.setDate(dto.getDate());
        entity.setInTime(parseTime(dto.getInTime()));
        entity.setOutTime(parseTime(dto.getOutTime()));
        entity.setLateIn(dto.getLateIn());
        entity.setErlOut(dto.getErlOut());
        entity.setOverTime(dto.getOverTime());
        entity.setRemark(dto.getRemark());

        // Calculate work hours
        if (entity.getInTime() != null && entity.getOutTime() != null) {
            double hours = Duration.between(entity.getInTime(), entity.getOutTime()).toMinutes() / 60.0;
            entity.setWorkHours(hours);
        } else {
            entity.setWorkHours(0.0);
        }

        // Status logic
        if (entity.getInTime() != null) entity.setStatus(AttendanceStatus.PRESENT);
        else entity.setStatus(AttendanceStatus.ABSENT);

        entity.setPunchOutEmailSent(false);
        return entity;
    }
    private List<AttendanceRequestDTO> parseExcelToDTO(byte[] excelData, LocalDate date) throws Exception {
        List<AttendanceRequestDTO> list = new ArrayList<>();
        try (InputStream inputStream = new ByteArrayInputStream(excelData);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String empId = getCellValue(row.getCell(1), evaluator);
                if (empId == null || empId.isBlank() || empId.contains("Total")) continue;

                AttendanceRequestDTO dto = new AttendanceRequestDTO();
                dto.setEmployeeId(empId);
                dto.setEmployeeName(getCellValue(row.getCell(2), evaluator));
                dto.setShift(getCellValue(row.getCell(3), evaluator));
                dto.setInTime(normalize(getCellValue(row.getCell(4), evaluator)));
                dto.setLateIn(normalize(getCellValue(row.getCell(5), evaluator)));
                dto.setErlOut(normalize(getCellValue(row.getCell(6), evaluator)));
                dto.setOutTime(normalize(getCellValue(row.getCell(7), evaluator)));
                dto.setOverTime(normalize(getCellValue(row.getCell(9), evaluator)));
                dto.setRemark(getCellValue(row.getCell(11), evaluator));
                dto.setDate(date);
                list.add(dto);
            }
        }
        return list;
    }
    private String normalize(String val) {
        if (val == null) return null;
        val = val.trim();
        return (val.isEmpty() || val.equals("--:--") || val.equals("00:00")) ? null : val;
    }
    // 🔹 Helpers
    private String normalizeCellValue(String val) {
        if (val == null) return null;
        val = val.trim();
        if (val.isEmpty() || val.equalsIgnoreCase("INTime")
                || val.equalsIgnoreCase("OUTTime") || val.equals("--:--")
                || val.equals("00:00") || val.equals("0")) {
            return null;
        }
        return val;
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : String.valueOf(cell.getNumericCellValue());
            case FORMULA -> {
                CellType t = evaluator.evaluateFormulaCell(cell);
                if (t == CellType.STRING) yield cell.getStringCellValue().trim();
                else if (t == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                else yield null;
            }
            default -> null;
        };
    }
    private LocalTime parseTime(String time) {
        try {
            return time == null ? null : LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> getEmployeesFromAttendance() {
        List<EmployeeAttendance> attendanceList = importDailyAttendanceFromEtimeOffice();
        List<Map<String, Object>> employees = new ArrayList<>();

        for (EmployeeAttendance attendance : attendanceList) {
            Map<String, Object> empData = new HashMap<>();
            empData.put("employeeId", attendance.getEmployeeId());
            empData.put("name", attendance.getEmployeeName());
            // fetch email from your config
            String email = employeeEmailConfig.getEmailByEmployeeId(attendance.getEmployeeId());
            empData.put("email", email);
            employees.add(empData);
        }
        return employees;
    }

    @Override
    public AttendanceSummaryDTO getMonthlyAttendanceSummary(String employeeId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // 1️⃣ Fetch attendance records from DB
        List<EmployeeAttendance> records =
                repository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);

        // 2️⃣ Fetch all approved leaves for the employee and filter by month
        List<Map<String, Object>> leaves = leaveClient.getApprovedLeaves(employeeId);
        List<Map<String, Object>> filteredLeaves = leaveClient.filterLeavesByDate(leaves, startDate, endDate);

        // Convert leave periods to a set of LocalDates for easy lookup
        Set<LocalDate> leaveDates = filteredLeaves.stream()
                .flatMap(l -> {
                    LocalDate s = LocalDate.parse((String) l.get("startDate"));
                    LocalDate e = LocalDate.parse((String) l.get("endDate"));
                    return s.datesUntil(e.plusDays(1)); // inclusive
                })
                .collect(Collectors.toSet());

        // 3️⃣ Fetch holidays in the month
        List<Map<String, Object>> holidays = leaveClient.getHolidays(startDate, endDate);
        Set<LocalDate> holidayDates = holidays.stream()
                .map(h -> LocalDate.parse((String) h.get("date")))
                .collect(Collectors.toSet());

        // 4️⃣ Initialize counters
        int present = 0;
        int halfDay = 0;
        int absent = 0;
        int pendingPunches = 0;
        int leaveCount = leaveDates.size();
        int holidayCount = holidayDates.size();
        double totalWorkingHours = 0;

        // 5️⃣ Loop through each working day
        int actualWorkingDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // Skip weekends
            if (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                    date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                continue;
            }

            actualWorkingDays++;

            // Skip holidays
            if (holidayDates.contains(date)) continue;

            // Skip approved leaves (but count them separately)
            if (leaveDates.contains(date)) continue;

            // Find attendance for this date
            LocalDate finalDate = date;
            EmployeeAttendance record = records.stream()
                    .filter(r -> r.getDate().equals(finalDate))
                    .findFirst()
                    .orElse(null);

            if (record == null) {
                absent++;
            } else {
                if (AttendanceStatus.PRESENT.equals(record.getStatus())) {
                    present++;
                    if (record.getInTime() == null || record.getOutTime() == null) {
                        pendingPunches++;
                        totalWorkingHours += 8; // default full day
                    } else {
                        totalWorkingHours += java.time.Duration.between(record.getInTime(), record.getOutTime()).toHours();
                    }
                } else if (AttendanceStatus.HALF_DAY.equals(record.getStatus())) {
                    halfDay++;
                    totalWorkingHours += 4;
                } else if (AttendanceStatus.ABSENT.equals(record.getStatus())) {
                    absent++;
                }
            }
        }

        // 6️⃣ Build DTO including leaveCount
        return new AttendanceSummaryDTO(
                employeeId,
                month,
                year,
                actualWorkingDays,
                present,
                halfDay,
                absent,
                leaveCount,   // ✅ include approved leaves
                leaveDates.stream().map(LocalDate::toString).collect(Collectors.toList()),
                holidayCount,
                pendingPunches,
                totalWorkingHours
        );
    }


    public byte[] downloadMonthlyReport(String monthYear, List<String> employeeIds, String token) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            for (String empId : employeeIds) {
                body.add("empList[]", empId);
            }
            body.add("reportDate", monthYear);
            body.add("reportName", "MP");
            body.add("reportType", "PDF");
            body.add("shortType", "By Department Wise");
            body.add("__RequestVerificationToken", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            String url = "https://www.etimeoffice.com/MonthReportDownload/DetailsWeb";

            // First try to get raw bytes
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, request, byte[].class);

            if (response.getBody() == null || response.getBody().length == 0) {
                throw new RuntimeException("Monthly report API returned empty response");
            }

            byte[] bodyBytes = response.getBody();

            // Try to detect JSON (starts with {)
            String bodyAsString = new String(bodyBytes);
            if (bodyAsString.trim().startsWith("{")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(bodyAsString);
                if (root.has("_dataStr")) {
                    return Base64.getDecoder().decode(root.get("_dataStr").asText());
                }
            }

            // Otherwise assume it's already PDF
            return bodyBytes;

        } catch (Exception e) {
            throw new RuntimeException("Failed to download monthly report: " + e.getMessage(), e);
        }
    }

    // --- Public wrapper: downloads PDF then parses to structured JSON ---
    @Override
    public Map<String, Object> downloadMonthlyReportAsJson(String monthYear, List<String> employeeIds) {
        try {
            // Reuse your existing downloadMonthlyReport which returns PDF bytes
            byte[] pdfData = downloadMonthlyReport(monthYear, employeeIds);
            if (pdfData == null || pdfData.length == 0) {
                throw new RuntimeException("Monthly report PDF is empty");
            }
            return parseMonthlyPdfToMap(pdfData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download/parse monthly report: " + e.getMessage(), e);
        }
    }

    // --- PDF parsing: extract text and build a structured Map ---
    private Map<String, Object> parseMonthlyPdfToMap(byte[] pdfData) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            String[] lines = text.split("\\r?\\n");

            // Patterns - tweak these if your PDF uses different labels
            Pattern empIdP = Pattern.compile("Employee\\s*Id\\s*[:\\-]?\\s*(\\S+)", Pattern.CASE_INSENSITIVE);
            Pattern empNameP = Pattern.compile("Employee\\s*Name\\s*[:\\-]?\\s*(.+)", Pattern.CASE_INSENSITIVE);
            Pattern workingP = Pattern.compile("Total\\s*Working\\s*Days\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern presentP = Pattern.compile("\\bPresent\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern absentP = Pattern.compile("\\bAbsent\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern halfDayP = Pattern.compile("\\bHalf\\s*Day\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern pendP = Pattern.compile("(Pending\\s*Punches|Pending\\s*Punch)\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern perfP = Pattern.compile("\\bPerformance\\s*[:\\-]?\\s*(.+)", Pattern.CASE_INSENSITIVE);
            Pattern monthP = Pattern.compile("(Month|Report Date|Report)\\s*[:\\-]?\\s*(.+)", Pattern.CASE_INSENSITIVE);

            // Secondary: collect generic key/values like "Key : Value"
            Pattern kvP = Pattern.compile("^\\s*([A-Za-z ]{2,30})\\s*[:\\-]\\s*(.+)$");

            // Keep raw lines too (fallback)
            List<String> rawLines = new ArrayList<>();

            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty()) continue;
                rawLines.add(l);

                // Try all strong patterns
                Matcher m;
                m = empIdP.matcher(l);
                if (m.find() && !result.containsKey("employeeId")) result.put("employeeId", m.group(1).trim());

                m = empNameP.matcher(l);
                if (m.find() && !result.containsKey("employeeName")) result.put("employeeName", m.group(1).trim());

                m = workingP.matcher(l);
                if (m.find() && !result.containsKey("actualWorkingDays"))
                    result.put("actualWorkingDays", Integer.parseInt(m.group(1)));

                m = presentP.matcher(l);
                if (m.find() && !result.containsKey("present")) result.put("present", Integer.parseInt(m.group(1)));

                m = absentP.matcher(l);
                if (m.find() && !result.containsKey("absent")) result.put("absent", Integer.parseInt(m.group(1)));

                m = halfDayP.matcher(l);
                if (m.find() && !result.containsKey("halfDay")) result.put("halfDay", Integer.parseInt(m.group(1)));

                m = pendP.matcher(l);
                if (m.find() && !result.containsKey("pendingPunches"))
                    result.put("pendingPunches", Integer.parseInt(m.group(2)));

                m = perfP.matcher(l);
                if (m.find() && !result.containsKey("performance")) result.put("performance", m.group(1).trim());

                m = monthP.matcher(l);
                if (m.find() && !result.containsKey("reportDate")) result.put("reportDate", m.group(2).trim());

                // fallback key:value parsing
                Matcher kv = kvP.matcher(l);
                if (kv.find()) {
                    String key = ((Matcher) kv).group(1).trim().replaceAll("\\s+", "_").toLowerCase();
                    String val = kv.group(2).trim();
                    if (!result.containsKey(key)) {
                        // try numeric
                        try {
                            result.put(key, Integer.parseInt(val));
                        } catch (Exception ex) {
                            result.put(key, val);
                        }
                    }
                }
            }

            // If structured fields not found, add raw lines to result so you always return something useful
            if (!result.containsKey("employeeId") && !rawLines.isEmpty()) {
                result.put("rawText", rawLines);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("PDF parsing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public DailySummaryDTO getPresentAbsentSummary(LocalDate date) {
        List<EmployeeAttendance> records = repository.findByDate(date);

        long presentCount = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT)
                .count();

        long absentCount = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.ABSENT)
                .count();

        return new DailySummaryDTO(
                date.toString(),
                presentCount,
                absentCount
        );
    }


    @Transactional
    public List<EmployeeAttendance> importSmartMonthlyAttendance(String monthYear, LocalDate companyStartDate) {
        try {
            YearMonth ym = YearMonth.parse(monthYear, DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            if (start.isBefore(companyStartDate)) start = companyStartDate;

            List<EmployeeAttendance> allRecords = new ArrayList<>();

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                boolean exists = repository.existsByDate(date);

                if (!exists) {
                    try {
                        allRecords.addAll(importAttendanceFromEtimeOffice(date));
                        System.out.println("✅ Imported: " + date);
                    } catch (Exception ex) {
                        System.err.println("⚠️ Failed for " + date + ": " + ex.getMessage());
                    }
                } else {
                    System.out.println("ℹ️ Skipped existing date: " + date);
                }
            }
            return allRecords;
        } catch (Exception e) {
            throw new RuntimeException("Failed smart monthly import: " + e.getMessage(), e);
        }
    }

    public List<EmployeeAttendance> importAttendanceByDate(LocalDate date) throws Exception {
        byte[] excelData = etimeOfficeService.downloadReportForDate(date);

        if (excelData == null || excelData.length == 0) {
            throw new RuntimeException("No data from EtimeOffice for " + date);
        }

        List<AttendanceRequestDTO> dailyList = parseExcelToDTO(excelData, date);
        List<EmployeeAttendance> saved = new ArrayList<>();

        for (AttendanceRequestDTO dto : dailyList) {
            if (!repository.existsByEmployeeIdAndDate(dto.getEmployeeId(), dto.getDate())) {
                EmployeeAttendance entity = mapDtoToEntity(dto);
                entity.setSourceType("DAILY");
                repository.save(entity);
                saved.add(entity);
            }
        }

        return saved;
    }

    @Override
    public List<EmployeeAttendance> getAllImportedAttendance(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            // Filter by date range
            return repository.findByDateBetween(startDate, endDate);
        } else {
            // Return all records if no date range is passed
            return repository.findAll();
        }
    }
    @Override
    public List<EmployeeAttendance> getAttendanceByEmployeeAndDuration(String employeeId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            // ✅ Filter by employee and date range
            return repository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);
        } else {
            // ✅ Return all records for that employee if no date range provided
            return repository.findByEmployeeId(employeeId);
        }
    }
    @Override
    public Map<String, Object> getMonthlySummaryForEmployee(String employeeId, String month) {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            // 1️⃣ Map employee IDs
            String attendanceEmpId = employeeId;
            String leaveEmpId = employeeId;

            EmployeeMappingResponse mapping = employeeMappingClient.getByAnyId(employeeId);
            if (mapping != null) {
                if (mapping.getAttendanceEmpId() != null) attendanceEmpId = mapping.getAttendanceEmpId();
                if (mapping.getLeaveEmpUuid() != null) leaveEmpId = mapping.getLeaveEmpUuid();
            }

            // 2️⃣ Parse month
            YearMonth ym = YearMonth.parse(month);
            LocalDate startDate = ym.atDay(1);
            LocalDate endDate = ym.atEndOfMonth();

            // 3️⃣ Fetch attendance records
            List<EmployeeAttendance> records = repository.findByEmployeeIdAndDateBetween(attendanceEmpId, startDate, endDate);
            Map<LocalDate, List<EmployeeAttendance>> byDate = records.stream()
                    .collect(Collectors.groupingBy(EmployeeAttendance::getDate));

            // 4️⃣ Fetch approved leaves
            List<Map<String, Object>> leaves = leaveClient.getApprovedLeaves(leaveEmpId);
            Set<LocalDate> leaveDates = new HashSet<>();
            List<String> leaveDatesList = new ArrayList<>();
            for (Map<String, Object> l : leaves) {
                LocalDate leaveStart = LocalDate.parse((String) l.get("startDate"));
                LocalDate leaveEnd = LocalDate.parse((String) l.get("endDate"));

                if (leaveEnd.isBefore(startDate) || leaveStart.isAfter(endDate)) continue;

                LocalDate from = leaveStart.isBefore(startDate) ? startDate : leaveStart;
                LocalDate to = leaveEnd.isAfter(endDate) ? endDate : leaveEnd;

                from.datesUntil(to.plusDays(1)).forEach(date -> {
                    leaveDates.add(date);
                    leaveDatesList.add(date.toString());
                });
            }

            // 5️⃣ Fetch holidays
            List<Map<String, Object>> holidays = leaveClient.getHolidays(startDate, endDate);
            Set<LocalDate> holidayDates = holidays.stream()
                    .map(h -> LocalDate.parse((String) h.get("date")))
                    .collect(Collectors.toSet());

            // 6️⃣ Initialize counters
            int totalWorkingDays = 0;
            int presentDays = 0;
            int absentDays = 0;
            int pendingPunches = 0;
            double totalWorkHours = 0.0;
            List<String> pendingPunchDates = new ArrayList<>();

            // 7️⃣ Loop through each day of the month
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                List<EmployeeAttendance> dayRecords = byDate.getOrDefault(date, Collections.emptyList());

                boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                boolean isHolidayOrLeave = holidayDates.contains(date) || leaveDates.contains(date);

                // Count working days only for weekdays excluding holidays/leaves
                if (!isWeekend && !isHolidayOrLeave) totalWorkingDays++;

                // If no attendance records on a working day, count absent
                if (!isWeekend && !isHolidayOrLeave && dayRecords.isEmpty()) {
                    absentDays++;
                    continue;
                }

                // 8️⃣ Process punches for the day
                boolean dayHasCompletePunch = false;    // at least one record with in & out
                boolean dayHasIncompletePunch = false;  // at least one record with in but no out
                double dayHours = 0.0;

                for (EmployeeAttendance r : dayRecords) {
                    boolean hasIn = r.getInTime() != null;
                    boolean hasOut = r.getOutTime() != null;

                    if (hasIn && hasOut) {
                        dayHasCompletePunch = true;
                        dayHours += (r.getWorkHours() != null && r.getWorkHours() > 0)
                                ? r.getWorkHours()
                                : Duration.between(r.getInTime(), r.getOutTime()).toMinutes() / 60.0;
                    } else if (hasIn && !hasOut) {
                        dayHasIncompletePunch = true;
                    }
                }

                // Count present days only for working days
                if (!isWeekend && !isHolidayOrLeave && dayHasCompletePunch) presentDays++;

                // Count pending punches if at least one incomplete punch exists and no complete punch
                if (dayHasIncompletePunch && !dayHasCompletePunch) {
                    pendingPunches++;
                    pendingPunchDates.add(date.toString());
                }

                totalWorkHours += dayHours;
            }

            double avgWorkHours = presentDays > 0 ? totalWorkHours / presentDays : 0.0;

            // 9️⃣ Build summary
            summary.put("month", month);
            summary.put("employeeId", employeeId);
            summary.put("attendanceEmployeeId", attendanceEmpId);
            summary.put("leaveEmployeeId", leaveEmpId);
            summary.put("actualWorkingDays", totalWorkingDays);
            summary.put("presentDays", presentDays);
            summary.put("absentDays", absentDays);
            summary.put("leaveDays", leaveDates.size());
            summary.put("leaveDates", leaveDatesList);
            summary.put("pendingPunches", pendingPunches);
            summary.put("pendingPunchDates", pendingPunchDates);
            summary.put("totalWorkHours", String.format("%.2f", totalWorkHours));
            summary.put("averageWorkHours", String.format("%.2f", avgWorkHours));

        } catch (Exception e) {
            e.printStackTrace();
            summary.put("error", e.getMessage());
        }

        return summary;
    }






    @Override
    public List<EmployeeAttendance> employeeMonthlyDetails(String employeeId, LocalDate startDate, LocalDate endDate) {
        String finalEmpId = employeeId;
        try {
            EmployeeMappingResponse mapping = employeeMappingClient.getByEmployeeUuid(employeeId);
            if (mapping != null && mapping.getAttendanceEmpId() != null) {
                System.out.println("✅ Mapping found: " + employeeId + " → " + mapping.getAttendanceEmpId());
                finalEmpId = mapping.getAttendanceEmpId();
            } else {
                System.out.println("⚠️ No attendance mapping found for: " + employeeId);
            }
        } catch (Exception e) {
            System.out.println("❌ Error while mapping employeeId: " + e.getMessage());
        }

        List<EmployeeAttendance> records = repository.findByEmployeeIdAndDateBetween(finalEmpId, startDate, endDate);
        System.out.println("📅 Found " + records.size() + " attendance records for ID: " + finalEmpId);
        return records;
    }


    @Override
    public EmployeeAttendance getAttendanceForEmployeeOnDate(String employeeId, LocalDate date) {
        String finalEmpId = employeeId;

        // 🔹 Step 1: Try to map UUID → attendanceEmpId if possible
        try {
            EmployeeMappingResponse mapping = employeeMappingClient.getByEmployeeUuid(employeeId);
            if (mapping != null && mapping.getAttendanceEmpId() != null) {
                finalEmpId = mapping.getAttendanceEmpId();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Failed to map employeeId: " + e.getMessage());
        }

        // 🔹 Step 2: Use finalEmpId to fetch attendance
        return repository.findByEmployeeIdAndDate(finalEmpId, date).orElse(null);
    }



}
