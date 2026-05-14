package com.configserver.hrm.employeeService.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordEmail(String toEmail, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your Account Password - Employee HRM System");
        message.setText("Dear User,\n\nYour account has been created successfully.\n\n" +
                "Your password is: " + password +
                "\n\nPlease login and change your password for security reasons." +
                "\n\nBest Regards,\nHRM Team");
        mailSender.send(message);
    }

    public void sendOTPEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset OTP - Employee HRM System");
        message.setText("Dear User,\n\nYou have requested to reset your password.\n\n" +
                "Your OTP for password reset is: " + otp +
                "\n\nThis OTP is valid for 10 minutes." +
                "\n\nIf you didn't request this, please ignore this email." +
                "\n\nBest Regards,\nHRM Team");
        mailSender.send(message);
    }

    public void sendPasswordResetConfirmation(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Successful - Employee HRM System");
        message.setText("Dear User,\n\nYour password has been reset successfully.\n\n" +
                "If you didn't make this change, please contact our support team immediately." +
                "\n\nBest Regards,\nHRM Team");
        mailSender.send(message);
    }
}