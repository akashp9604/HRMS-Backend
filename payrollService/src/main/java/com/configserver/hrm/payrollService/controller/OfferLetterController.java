package com.configserver.hrm.payrollService.controller;

import com.configserver.hrm.payrollService.entity.OfferLetterStatus;
import com.configserver.hrm.payrollService.repository.OfferLetterStatusRepository;
import com.configserver.hrm.payrollService.service.EmailService;
import com.configserver.hrm.payrollService.service.OfferLetterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll/offer-letter")
@CrossOrigin(origins = "http://localhost:3000")
public class OfferLetterController {

    @Autowired
    private OfferLetterService offerLetterService;

    @Autowired
    private OfferLetterStatusRepository offerLetterStatusRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateOfferLetter(@RequestParam String employeeId) throws Exception {
        byte[] pdfBytes = offerLetterService.generateOfferLetter(employeeId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Offer_Letter_" + employeeId + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // ✅ UPDATED: Send offer email with ONLY acceptance link
    @PostMapping("/send-offer")
    public ResponseEntity<?> sendOfferLetter(@RequestParam UUID employeeId) {
        try {
            System.out.println("📧 Sending offer email for employee: " + employeeId);

            // Create or update offer status
            OfferLetterStatus status = offerLetterStatusRepository.findByEmployeeId(employeeId)
                    .orElse(OfferLetterStatus.builder()
                            .employeeId(employeeId)
                            .accepted(false)
                            .build());

            String acceptanceLink = "http://localhost:3000/offer-acceptance?employeeId=" + employeeId;

            // For now using default email - in production, fetch from employee service
            String defaultEmail = "employee@configserverllp.com";
            String defaultName = "Employee";

            // ✅ SEND ONLY ACCEPTANCE LINK (no download in initial email)
            boolean emailSent = emailService.sendOfferEmail(employeeId, defaultEmail, defaultName, acceptanceLink);

            // Save status
            offerLetterStatusRepository.save(status);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "Offer letter sent successfully",
                    "emailSent", emailSent
            ));

        } catch (Exception e) {
            System.err.println("❌ Error sending offer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send offer: " + e.getMessage()));
        }
    }

    // Check acceptance status
    @GetMapping("/status/{employeeId}")
    public ResponseEntity<?> getOfferStatus(@PathVariable UUID employeeId) {
        try {
            OfferLetterStatus status = offerLetterStatusRepository.findByEmployeeId(employeeId)
                    .orElse(OfferLetterStatus.builder()
                            .employeeId(employeeId)
                            .accepted(false)
                            .build());

            return ResponseEntity.ok().body(Map.of(
                    "employeeId", employeeId,
                    "accepted", status.isAccepted(),
                    "acceptedAt", status.getAcceptedAt()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get status: " + e.getMessage()));
        }
    }

    // ✅ ENHANCED: Accept offer and send download link via email
    @PostMapping("/accept")
    public ResponseEntity<?> acceptOffer(@RequestParam UUID employeeId) {
        try {
            System.out.println("🎉 Accepting offer for employee: " + employeeId);

            Optional<OfferLetterStatus> existingStatus = offerLetterStatusRepository.findByEmployeeId(employeeId);

            OfferLetterStatus status = existingStatus.orElse(
                    OfferLetterStatus.builder()
                            .employeeId(employeeId)
                            .accepted(false)
                            .build()
            );

            if (!status.isAccepted()) {
                status.setAccepted(true);
                offerLetterStatusRepository.save(status);

                // ✅ SEND DOWNLOAD LINK VIA EMAIL AFTER ACCEPTANCE
                if (status.getEmployeeEmail() != null && !status.getEmployeeEmail().isEmpty()) {
                    boolean emailSent = emailService.sendAcceptanceConfirmationEmail(
                            employeeId,
                            status.getEmployeeEmail(),
                            status.getEmployeeName() != null ? status.getEmployeeName() : "Employee"
                    );

                    System.out.println("📧 Acceptance confirmation email sent: " + emailSent);

                    return ResponseEntity.ok().body(Map.of(
                            "success", true,
                            "message", "Offer accepted successfully! Check your email for download link.",
                            "emailSent", emailSent
                    ));
                } else {
                    return ResponseEntity.ok().body(Map.of(
                            "success", true,
                            "message", "Offer accepted successfully! Contact HR for offer letter.",
                            "emailSent", false
                    ));
                }
            } else {
                return ResponseEntity.ok().body(Map.of(
                        "success", true,
                        "message", "Offer was already accepted",
                        "emailSent", false
                ));
            }

        } catch (Exception e) {
            System.err.println("❌ Error accepting offer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to accept offer: " + e.getMessage()));
        }
    }

    // ✅ ENHANCED: Download offer letter with STRICT acceptance check
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadOfferLetter(@RequestParam UUID employeeId) {
        try {
            System.out.println("=== 📥 DOWNLOAD REQUEST ===");
            System.out.println("Employee ID: " + employeeId);

            // ✅ STRICT CHECK: Offer must be accepted
            boolean isAccepted = offerLetterStatusRepository.findByEmployeeId(employeeId)
                    .map(OfferLetterStatus::isAccepted)
                    .orElse(false);

            System.out.println("Offer accepted status: " + isAccepted);

            if (!isAccepted) {
                System.err.println("❌ Download blocked - offer not accepted");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Offer must be accepted before downloading. Please accept the offer first.".getBytes());
            }

            System.out.println("✅ Generating offer letter PDF...");
            byte[] pdfBytes = offerLetterService.generateOfferLetter(employeeId.toString());

            if (pdfBytes == null || pdfBytes.length == 0) {
                System.err.println("❌ Generated PDF is empty");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Error generating PDF".getBytes());
            }

            System.out.println("✅ PDF generated successfully, size: " + pdfBytes.length + " bytes");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Offer_Letter_" + employeeId + ".pdf");

            System.out.println("✅ Sending PDF response");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("❌ Error in download: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error downloading offer letter: " + e.getMessage()).getBytes());
        }
    }

    // ✅ ENHANCED: Store employee email for sending download link later
    @PostMapping("/send")
    public ResponseEntity<?> sendOfferLetterWithEmail(
            @RequestBody Map<String, String> request) {
        try {
            UUID employeeId = UUID.fromString(request.get("employeeId"));
            String employeeEmail = request.get("employeeEmail");
            String employeeName = request.get("employeeName");

            System.out.println("📧 Sending offer to: " + employeeEmail + " for " + employeeName);

            // Create or update offer status WITH EMAIL
            OfferLetterStatus status = offerLetterStatusRepository.findByEmployeeId(employeeId)
                    .orElse(OfferLetterStatus.builder()
                            .employeeId(employeeId)
                            .accepted(false)
                            .build());

            // ✅ STORE EMAIL FOR SENDING DOWNLOAD LINK AFTER ACCEPTANCE
            status.setEmployeeEmail(employeeEmail);
            status.setEmployeeName(employeeName);

            String acceptanceLink = "http://localhost:3000/offer-acceptance?employeeId=" + employeeId;

            // ✅ SEND ONLY ACCEPTANCE LINK (no download in initial email)
            boolean emailSent = emailService.sendOfferEmail(employeeId, employeeEmail, employeeName, acceptanceLink);

            // Save status with email info
            offerLetterStatusRepository.save(status);

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "Offer letter sent successfully to " + employeeEmail,
                    "employeeId", employeeId.toString(),
                    "email", employeeEmail,
                    "emailSent", emailSent
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send offer: " + e.getMessage()));
        }
    }

    // ✅ Test endpoint
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestParam String testEmail) {
        try {
            System.out.println("🧪 Testing email configuration to: " + testEmail);

            boolean emailSent = emailService.sendOfferEmail(
                    UUID.randomUUID(),
                    testEmail,
                    "Test Employee",
                    "http://localhost:3000/offer-acceptance?employeeId=test"
            );

            if (emailSent) {
                return ResponseEntity.ok().body(Map.of(
                        "success", true,
                        "message", "Test email sent successfully to " + testEmail
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to send test email"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Test email failed: " + e.getMessage()));
        }
    }
}