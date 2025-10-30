package com.defecttracker.repository;

import com.defecttracker.entity.VaptTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaptTestCaseRepository extends JpaRepository<VaptTestCase, Long> {

    List<VaptTestCase> findByVaptReportId(Long vaptReportId);

    @Query("SELECT vtc FROM VaptTestCase vtc WHERE vtc.vaptReport.id = :vaptReportId ORDER BY vtc.testPlan.testCaseId")
    List<VaptTestCase> findByVaptReportIdOrdered(@Param("vaptReportId") Long vaptReportId);
}
