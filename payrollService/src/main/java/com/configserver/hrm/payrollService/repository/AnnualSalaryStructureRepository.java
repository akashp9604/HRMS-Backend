    package com.configserver.hrm.payrollService.repository;

    import com.configserver.hrm.payrollService.entity.AnnualSalaryStructure;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.util.List;
    import java.util.Optional;

    public interface AnnualSalaryStructureRepository extends JpaRepository<AnnualSalaryStructure, Long> {
        Optional<AnnualSalaryStructure> findByEmployeeIdAndFinancialYear(Long employeeId, String financialYear);
        List<AnnualSalaryStructure> findByEmployeeId(Long employeeId);
        List<AnnualSalaryStructure> findByAddedToOfferLetter(boolean addedToOfferLetter);
        List<AnnualSalaryStructure> findByStatus(String status);
    }