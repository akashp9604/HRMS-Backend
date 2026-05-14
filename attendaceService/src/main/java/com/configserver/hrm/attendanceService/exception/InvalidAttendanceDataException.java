package com.configserver.hrm.attendanceService.exception;

public class InvalidAttendanceDataException extends RuntimeException {

    // Constructor with message
    public InvalidAttendanceDataException(String message) {
        super(message);
    }

    // Constructor with message and cause
    public InvalidAttendanceDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
