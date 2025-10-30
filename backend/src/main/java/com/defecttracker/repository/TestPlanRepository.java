package com.defecttracker.repository;

import com.defecttracker.entity.TestPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestPlanRepository extends JpaRepository<TestPlan, Long> {

    Optional<TestPlan> findByTestCaseId(String testCaseId);

    List<TestPlan> findBySeverity(TestPlan.Severity severity);

    List<TestPlan> findByVulnerabilityNameContainingIgnoreCase(String vulnerabilityName);

    @Query("SELECT t FROM TestPlan t WHERE " +
           "LOWER(t.vulnerabilityName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<TestPlan> searchTestPlans(@Param("search") String search);

    @Query("SELECT COUNT(t) FROM TestPlan t")
    long countTotalTestPlans();

    @Query("SELECT MAX(CAST(SUBSTRING(t.testCaseId, 4) AS long)) FROM TestPlan t")
    Long findMaxTestCaseIdNumber();
}
