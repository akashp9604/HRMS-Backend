package com.configserver.hrm.attendanceService.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
@Component
public class PlaywrightMonthlyReportDownloader {

    private final String LOGIN_URL = "https://www.etimeoffice.com/Login/loginCheck";
    private final String REPORT_URL = "https://www.etimeoffice.com/MonthReportDownload/DetailsWeb";

    private final String CORPORATE_ID = "ConfigServer";
    private final String USERNAME = "ConfigServer";
    private final String PASSWORD = "ConfigServer@22";

    public void downloadMonthlyExcel(String monthYear, String downloadPath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(true));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setAcceptDownloads(true));

            Page page = context.newPage();

            // 1️⃣ Navigate to login page
            page.navigate(LOGIN_URL);

            // 2️⃣ Login
            page.fill("input[name='loginModel.corporateId']", CORPORATE_ID);
            page.fill("input[name='loginModel.userName']", USERNAME);
            page.fill("input[name='loginModel.password']", PASSWORD);
            page.click("button[type='submit']");

            // 3️⃣ Wait for dashboard to load completely
            page.waitForNavigation((Runnable) new Page.WaitForNavigationOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));

            // 4️⃣ Navigate to Monthly Report page
            page.navigate(REPORT_URL);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // 5️⃣ Handle iframe if report input is inside one
            Frame frameToUse = (Frame) page;
            if (page.frames().size() > 1) { // assume first child frame has the inputs
                frameToUse = page.frames().get(1);
            }

            // 6️⃣ Wait for reportDate input and fill month/year
            frameToUse.locator("input[name='reportDate']")
                    .waitFor(new Locator.WaitForOptions().setTimeout(60000));
            frameToUse.locator("input[name='reportDate']").fill(monthYear);

            // 7️⃣ Select Excel and shortType options
            frameToUse.locator("select[name='reportType']").selectOption("EXCEL");
            frameToUse.locator("select[name='shortType']").selectOption("By Department Wise");

            Download download = page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(120_000), new Runnable() {
                            @Override
                            public void run() {
                                Frame finalFrameToUse = null;
                                finalFrameToUse.locator("button[id='generateReport']").click();
                            }
                        });

            browser.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download monthly Excel: " + e.getMessage(), e);
        }
    }
}
