package com.configserver.hrm.attendanceService.exception;

public class ExcelDownloadException extends RuntimeException {
    public ExcelDownloadException(String message) {
        super(message);
    }

    public ExcelDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
