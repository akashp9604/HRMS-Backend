package com.configserver.hrm.mappingService.repository;

import com.configserver.hrm.mappingService.entity.EmployeeIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeIdMappingRepository extends JpaRepository<EmployeeIdMapping, Long> {

    Optional<EmployeeIdMapping> findByAttendanceEmpId(String attendanceEmpId);
    Optional<EmployeeIdMapping> findByPayrollEmpUuid(String payrollUuid);
    Optional<EmployeeIdMapping> findByLeaveEmpUuid(String leaveUuid);
    Optional<EmployeeIdMapping> findByEmployeeServiceUuid(String employeeServiceUuid);
    Optional<EmployeeIdMapping> findByEmployeeName(String employeeName);
    Optional<EmployeeIdMapping> findByEmployeeNameIgnoreCase(String name);

    // ✅ Find by Annual Structure ID
    Optional<EmployeeIdMapping> findByAnnualStructureId(Long annualStructureId);

    // ✅ Mappings without annual structure
    @Query("SELECT m FROM EmployeeIdMapping m WHERE m.annualStructureId IS NULL")
    List<EmployeeIdMapping> findMappingsWithoutAnnualStructure();

    // ✅ Mappings with annual structure
    @Query("SELECT m FROM EmployeeIdMapping m WHERE m.annualStructureId IS NOT NULL")
    List<EmployeeIdMapping> findMappingsWithAnnualStructure();

    // ✅ Search by partial name
    List<EmployeeIdMapping> findByEmployeeNameContainingIgnoreCase(String name);

    // ✅ Fetch employee email by leave UUID (used by Leave Service)
    @Query("SELECT e.employeeEmail FROM EmployeeIdMapping e WHERE e.leaveEmpUuid = :leaveUuid")
    String findEmployeeEmailByLeaveUuid(@Param("leaveUuid") String leaveUuid);
}
