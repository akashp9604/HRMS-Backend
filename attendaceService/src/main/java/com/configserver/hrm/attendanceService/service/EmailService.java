package com.configserver.hrm.attendanceService.service;

public interface EmailService {
    void sendPunchOutReminder(String toEmail, String employeeName);
}
