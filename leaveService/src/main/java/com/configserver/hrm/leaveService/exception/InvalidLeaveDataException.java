    package com.configserver.hrm.leaveService.exception;

    public class InvalidLeaveDataException extends RuntimeException {
        public InvalidLeaveDataException(String message) {
            super(message);
        }
    }
