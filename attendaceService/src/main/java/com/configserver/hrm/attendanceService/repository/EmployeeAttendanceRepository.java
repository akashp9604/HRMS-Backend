package com.configserver.hrm.attendanceService.repository;

import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, String> {
    List<EmployeeAttendance> findByDate(LocalDate date);
   // List<EmployeeAttendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate from, LocalDate to);
    boolean existsByEmployeeIdAndDate(String employeeId, LocalDate date);
    @Transactional

    void deleteByDate(LocalDate date);
    boolean existsByDate(LocalDate date);
    @Transactional
    void deleteByEmployeeIdAndDate(String employeeId, LocalDate date);

    List<EmployeeAttendance> findByDateBetween(LocalDate start, LocalDate end);

    List<EmployeeAttendance> findByEmployeeId(String employeeId);

    List<EmployeeAttendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);

    Optional<EmployeeAttendance> findByEmployeeIdAndDate(String employeeId, LocalDate date);

    @Transactional
    @Modifying
    @Query("DELETE FROM EmployeeAttendance e WHERE e.employeeId = :employeeId AND e.date BETWEEN :startDate AND :endDate")
    void deleteByEmployeeIdAndDateBetween(@Param("employeeId") String employeeId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);


    @Query("SELECT COUNT(e) FROM EmployeeAttendance e WHERE e.date BETWEEN :startDate AND :endDate")
    long countByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}

