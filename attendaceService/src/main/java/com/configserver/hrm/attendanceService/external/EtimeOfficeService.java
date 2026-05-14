package com.configserver.hrm.attendanceService.external;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EtimeOfficeService {

    public final RestTemplate restTemplate;
    private final CookieStore cookieStore = new BasicCookieStore();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String LOGIN_URL = "https://www.etimeoffice.com/Login/loginCheck";
    private final String MONTHLY_REPORT_URL = "https://www.etimeoffice.com/MonthReportDownload/DetailsWeb";

    private final String CORPORATE_ID = "ConfigServer";
    private final String USERNAME = "ConfigServer";
    private final String PASSWORD = "ConfigServer@22";


    public EtimeOfficeService() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore) // keep cookies (JSESSIONID etc.)
                .build();
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    // 🔹 Download Daily Attendance Report (Excel)
    public byte[] downloadDailyReport(String reportDate) {
        try {
            // Step 1: Fetch login page -> extract token
            String loginPageUrl = "https://www.etimeoffice.com/Login/loginCheck";
            String loginPageHtml = restTemplate.getForObject(loginPageUrl, String.class);
            String token = extractToken(loginPageHtml);

            // Step 2: Prepare login request
            String loginUrl = "https://www.etimeoffice.com/Login/loginCheck";
            MultiValueMap<String, String> loginBody = new LinkedMultiValueMap<>();
            loginBody.add("loginModel.corporateId", "ConfigServer");
            loginBody.add("loginModel.userName", "ConfigServer");
            loginBody.add("loginModel.password", "ConfigServer@22");
            loginBody.add("pageTital", "Login Page");
            loginBody.add("__RequestVerificationToken", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Step 3: Perform login
            ResponseEntity<String> loginResp = restTemplate.postForEntity(
                    loginUrl,
                    new HttpEntity<>(loginBody, headers),
                    String.class
            );

            // Step 4: Get cookies from login response
            List<String> cookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null) {
                headers.put(HttpHeaders.COOKIE, cookies);
            }

            // Step 5: Call Daily Report API (GET)
            String reportUrl = "https://www.etimeoffice.com/DailyReport/DetailsWeb1";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(reportUrl)
                    .queryParam("reportDate", reportDate)
                    .queryParam("reportName", "DP")
                    .queryParam("reportType", "EXCEL")
                    .queryParam("shortType", "By Department Wise");

            HttpEntity<Void> reportEntity = new HttpEntity<>(headers);
            ResponseEntity<String> reportResp = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    reportEntity,
                    String.class
            );

            // Step 6: Parse JSON & decode Base64
            String bodyContent = reportResp.getBody();
            if (bodyContent == null) {
                throw new RuntimeException("Daily report API returned empty response");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(bodyContent);
            String base64Excel = root.get("_dataStr").asText();

            // Step 7: Decode Base64 -> raw Excel bytes
            return Base64.getDecoder().decode(base64Excel);

        } catch (Exception e) {
            throw new RuntimeException("Failed to download daily report: " + e.getMessage(), e);
        }
    }

    public byte[] downloadMonthlyReport(String monthYear, List<String> employeeIds) {
        try {
            // 1️⃣ Login and get cookies
            HttpHeaders loginHeaders = new HttpHeaders();
            loginHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> loginBody = new LinkedMultiValueMap<>();
            loginBody.add("loginModel.corporateId", CORPORATE_ID);
            loginBody.add("loginModel.userName", USERNAME);
            loginBody.add("loginModel.password", PASSWORD);
            loginBody.add("pageTital", "Login Page");

            ResponseEntity<String> loginResp = restTemplate.postForEntity(
                    LOGIN_URL,
                    new HttpEntity<>(loginBody, loginHeaders),
                    String.class
            );

            List<String> cookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies == null || cookies.isEmpty()) {
                throw new RuntimeException("Login failed: no cookies returned");
            }

            HttpHeaders headersWithCookies = new HttpHeaders();
            headersWithCookies.put(HttpHeaders.COOKIE, cookies);
            headersWithCookies.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 2️⃣ Prepare monthly report payload
            MultiValueMap<String, String> reportBody = new LinkedMultiValueMap<>();
            employeeIds.forEach(id -> reportBody.add("_empList[]", id));
            reportBody.add("reportDate", monthYear);
            reportBody.add("reportName", "MP");
            reportBody.add("reportType", "EXCEL"); // or PDF
            reportBody.add("shortType", "By Department Wise");
            // If __RequestVerificationToken is required, extract from loginResp and add here

            // 3️⃣ Call monthly report endpoint
            ResponseEntity<String> reportResp = restTemplate.postForEntity(
                    MONTHLY_REPORT_URL,
                    new HttpEntity<>(reportBody, headersWithCookies),
                    String.class
            );

            if (reportResp.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Monthly report API failed: " + reportResp.getStatusCode());
            }

            // 4️⃣ Parse JSON and get actual download URL (_dataStr)
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(reportResp.getBody());
            String dataStr = root.get("_dataStr").asText();
            if (dataStr == null || dataStr.isEmpty()) {
                throw new RuntimeException("Monthly report did not return download URL");
            }

            // 5️⃣ Call the actual download URL
            String downloadUrl = "https://www.etimeoffice.com" + dataStr;
            HttpHeaders downloadHeaders = new HttpHeaders();
            downloadHeaders.put(HttpHeaders.COOKIE, cookies);

            ResponseEntity<byte[]> downloadResp = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(downloadHeaders),
                    byte[].class
            );

            if (downloadResp.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to download report file");
            }

            return downloadResp.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Failed to download monthly report: " + e.getMessage(), e);
        }
    }


    public String extractToken(String html) {
        Pattern pattern = Pattern.compile("name=\"__RequestVerificationToken\".*?value=\"(.*?)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Verification token not found in login page");
    }
    public byte[] downloadReportForDate(LocalDate date) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return downloadDailyReport(formattedDate);
    }


}