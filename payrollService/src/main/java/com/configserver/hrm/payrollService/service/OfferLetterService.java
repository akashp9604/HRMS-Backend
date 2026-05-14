package com.configserver.hrm.payrollService.service;

public interface OfferLetterService {
    byte[] generateOfferLetter(String employeeId) throws Exception;

}
