package com.configserver.hrm.payrollService.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // ✅ INITIAL OFFER EMAIL: Only acceptance link, NO download
    public boolean sendOfferEmail(UUID employeeId, String employeeEmail, String employeeName, String acceptanceLink) {
        System.out.println("🚀 Sending initial offer email to: " + employeeEmail);

        try {
            if (employeeEmail == null || employeeEmail.trim().isEmpty()) {
                System.err.println("❌ Email address is null or empty");
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("admin@gmail.com");
            helper.setTo(employeeEmail);
            helper.setSubject("Offer Letter - Config Server LLP");

            String emailContent = buildOfferEmailContent(employeeName, acceptanceLink);
            helper.setText(emailContent, true);

            System.out.println("📧 Sending initial offer email to: " + employeeEmail);
            mailSender.send(message);

            System.out.println("✅ Initial offer email sent successfully");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send initial offer email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ✅ NEW: Send acceptance confirmation with direct download link
    public boolean sendAcceptanceConfirmationEmail(UUID employeeId, String employeeEmail, String employeeName) {
        System.out.println("🎉 Sending acceptance confirmation to: " + employeeEmail);

        try {
            if (employeeEmail == null || employeeEmail.trim().isEmpty()) {
                System.err.println("❌ Email address is null or empty");
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("admin@gmail.com");
            helper.setTo(employeeEmail);
            helper.setSubject("Offer Accepted - Download Your Offer Letter - Config Server LLP");

            // ✅ DIRECT DOWNLOAD LINK (no login required)
            String directDownloadLink = "http://localhost:8089/api/payroll/offer-letter/download?employeeId=" + employeeId;
            String emailContent = buildAcceptanceEmailContent(employeeName, directDownloadLink);
            helper.setText(emailContent, true);

            System.out.println("📧 Sending acceptance confirmation with download link");
            mailSender.send(message);

            System.out.println("✅ Acceptance confirmation with download link sent successfully");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to send acceptance confirmation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ✅ INITIAL OFFER EMAIL: Only acceptance button
    private String buildOfferEmailContent(String employeeName, String acceptanceLink) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { 
                    font-family: 'Arial', sans-serif; 
                    line-height: 1.6; 
                    color: #333;
                    margin: 0;
                    padding: 20px;
                    background-color: #f4f4f4;
                }
                .container { 
                    max-width: 600px; 
                    margin: 0 auto; 
                    padding: 30px;
                    border: 1px solid #ddd;
                    border-radius: 10px;
                    background-color: #ffffff;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                .header {
                    text-align: center;
                    background: linear-gradient(135deg, #007bff, #0056b3);
                    color: white;
                    padding: 30px;
                    border-radius: 10px 10px 0 0;
                    margin: -30px -30px 30px -30px;
                }
                .button { 
                    display: inline-block; 
                    padding: 15px 30px; 
                    background-color: #007bff; 
                    color: white; 
                    text-decoration: none; 
                    border-radius: 5px;
                    font-size: 16px;
                    font-weight: bold;
                    margin: 20px 0;
                    text-align: center;
                }
                .button:hover {
                    background-color: #0056b3;
                }
                .footer {
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #ddd;
                    color: #666;
                    text-align: center;
                }
                .info-box {
                    background-color: #f8f9fa;
                    padding: 15px;
                    border-left: 4px solid #007bff;
                    margin: 20px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Congratulations %s!</h1>
                    <p>Welcome to Config Server LLP</p>
                </div>
                
                <h2>🎉 Your Offer Letter is Ready!</h2>
                
                <p>Dear <strong>%s</strong>,</p>
                
                <p>We are delighted to offer you a position at Config Server LLP. Please accept the offer to access your official offer letter.</p>
                
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" class="button">
                        ✅ Accept Offer to Get Offer Letter
                    </a>
                </div>
                
                <div class="info-box">
                    <p><strong>Important Information:</strong></p>
                    <ul>
                        <li>You must accept the offer first to download your official offer letter</li>
                        <li>After acceptance, you will receive download link via email</li>
                        <li>You can download the PDF directly without any login</li>
                        <li>This link is unique to you and should not be shared</li>
                    </ul>
                </div>
                
                <p><strong>Note:</strong> Download option will be available only after offer acceptance.</p>
                
                <p>If you have any questions, please contact our HR team.</p>
                
                <div class="footer">
                    <p>Best regards,<br>
                    <strong>HR Department</strong><br>
                    Config Server LLP</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(employeeName, employeeName, acceptanceLink);
    }

    // ✅ ACCEPTANCE EMAIL: Includes direct download link
    private String buildAcceptanceEmailContent(String employeeName, String downloadLink) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { 
                    font-family: 'Arial', sans-serif; 
                    line-height: 1.6; 
                    color: #333;
                    margin: 0;
                    padding: 20px;
                    background-color: #f4f4f4;
                }
                .container { 
                    max-width: 600px; 
                    margin: 0 auto; 
                    padding: 30px;
                    border: 1px solid #ddd;
                    border-radius: 10px;
                    background-color: #ffffff;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                .header {
                    text-align: center;
                    background: linear-gradient(135deg, #28a745, #1e7e34);
                    color: white;
                    padding: 30px;
                    border-radius: 10px 10px 0 0;
                    margin: -30px -30px 30px -30px;
                }
                .button { 
                    display: inline-block; 
                    padding: 15px 30px; 
                    background-color: #28a745; 
                    color: white; 
                    text-decoration: none; 
                    border-radius: 5px;
                    font-size: 18px;
                    font-weight: bold;
                    margin: 20px 0;
                    text-align: center;
                }
                .button:hover {
                    background-color: #218838;
                    transform: translateY(-2px);
                    box-shadow: 0 4px 8px rgba(0,0,0,0.2);
                }
                .footer {
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #ddd;
                    color: #666;
                    text-align: center;
                }
                .download-section {
                    background-color: #e8f5e8;
                    padding: 25px;
                    border-radius: 8px;
                    margin: 25px 0;
                    text-align: center;
                    border: 2px dashed #28a745;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Offer Accepted Successfully! 🎉</h1>
                    <p>Welcome to the Config Server LLP Family</p>
                </div>
                
                <h2>Dear <strong>%s</strong>,</h2>
                
                <p>Thank you for accepting our offer! We are excited to welcome you to Config Server LLP.</p>
                
                <div class="download-section">
                    <h3>📄 Your Official Offer Letter is Ready for Download!</h3>
                    <p>Click the button below to download your official offer letter in PDF format.</p>
                    
                    <div style="text-align: center; margin: 25px 0;">
                        <a href="%s" class="button">
                            📥 Download Offer Letter PDF
                        </a>
                    </div>
                    
                    <p><strong>No login required!</strong> Click the button above to download immediately.</p>
                </div>
                
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p><strong>Next Steps:</strong></p>
                    <ul>
                        <li>Download and save your offer letter for your records</li>
                        <li>Review the terms and conditions carefully</li>
                        <li>Our HR team will contact you shortly for onboarding process</li>
                    </ul>
                </div>
                
                <p>If you have any questions or need assistance, please don't hesitate to contact our HR department.</p>
                
                <div class="footer">
                    <p>Welcome aboard!<br>
                    <strong>HR Department</strong><br>
                    Config Server LLP</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(employeeName, downloadLink);
    }
}