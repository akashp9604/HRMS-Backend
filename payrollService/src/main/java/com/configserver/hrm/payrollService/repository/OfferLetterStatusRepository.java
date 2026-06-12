// src/main/java/com/configserver/hrm/payrollService/repository/OfferLetterStatusRepository.java
package com.configserver.hrm.payrollService.repository;

import com.configserver.hrm.payrollService.entity.OfferLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OfferLetterStatusRepository extends JpaRepository<OfferLetterStatus, Long> {
    Optional<OfferLetterStatus> findByEmployeeId(Long employeeId); // Changed to UUID
    boolean existsByEmployeeIdAndAccepted(Long employeeId, boolean accepted); // Changed to UUID
}