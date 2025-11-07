package com.defecttracker.repository;

import com.defecttracker.entity.VaptTestCase;
import com.defecttracker.entity.TestPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VaptTestCaseRepository extends JpaRepository<VaptTestCase, Long> {

    @Query("SELECT vtc FROM VaptTestCase vtc WHERE vtc.vaptReport.id = :reportId ORDER BY vtc.testPlan.severity DESC, vtc.testPlan.vulnerabilityName ASC")
    List<VaptTestCase> findByVaptReportIdOrdered(@Param("reportId") Long reportId);

    List<VaptTestCase> findByVaptReportId(Long reportId);

    @Query("SELECT COUNT(vtc) FROM VaptTestCase vtc WHERE vtc.vulnerabilityStatus = :status")
    long countByVulnerabilityStatus(@Param("status") VaptTestCase.VulnerabilityStatus status);

    @Query("SELECT COUNT(vtc) FROM VaptTestCase vtc WHERE vtc.severity = :severity OR (vtc.severity IS NULL AND vtc.testPlan.severity = :severity)")
    long countBySeverity(@Param("severity") TestPlan.Severity severity);

    @Query("SELECT vtc FROM VaptTestCase vtc WHERE vtc.createdAt BETWEEN :startDate AND :endDate")
    List<VaptTestCase> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<VaptTestCase> findByTestPlanId(Long testPlanId);
}
