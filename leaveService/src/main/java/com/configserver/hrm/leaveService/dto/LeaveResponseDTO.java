package com.configserver.hrm.leaveService.dto;

import com.configserver.hrm.leaveService.entity.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveResponseDTO {

    private UUID id;
    private String employeeId;
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveStatus status;
    private String reason;
    private String approvedBy;
    private LocalDateTime appliedOn;
    private LocalDateTime approvedOn;
    private UUID medicalDocumentId;
    private String medicalDocName;
    private String medicalDocPath;

}
