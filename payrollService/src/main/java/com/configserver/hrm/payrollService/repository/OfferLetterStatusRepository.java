// src/main/java/com/configserver/hrm/payrollService/repository/OfferLetterStatusRepository.java
package com.configserver.hrm.payrollService.repository;

import com.configserver.hrm.payrollService.entity.OfferLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OfferLetterStatusRepository extends JpaRepository<OfferLetterStatus, Long> {
    Optional<OfferLetterStatus> findByEmployeeId(UUID employeeId); // Changed to UUID
    boolean existsByEmployeeIdAndAccepted(UUID employeeId, boolean accepted); // Changed to UUID
}