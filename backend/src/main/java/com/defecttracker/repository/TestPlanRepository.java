package com.defecttracker.repository;

import com.defecttracker.entity.TestPlan;
import com.defecttracker.entity.TestPlan.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestPlanRepository extends JpaRepository<TestPlan, Long> {

    List<TestPlan> findBySeverity(Severity severity);

    @Query("SELECT t FROM TestPlan t WHERE " +
           "LOWER(t.vulnerabilityName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.testCaseId) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<TestPlan> searchTestPlans(@Param("query") String query);

    Optional<TestPlan> findByTestCaseId(String testCaseId);

    @Query("SELECT COUNT(t) FROM TestPlan t")
    long countTotalTestPlans();

    @Query("SELECT MAX(CAST(SUBSTRING(t.testCaseId, 4) AS long)) FROM TestPlan t")
    Long findMaxTestCaseIdNumber();

    List<TestPlan> findAllById(Iterable<Long> ids);
}
