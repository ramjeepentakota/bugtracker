package com.defecttracker.repository;

import com.defecttracker.entity.Defect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DefectRepository extends JpaRepository<Defect, Long> {

    List<Defect> findByClientId(Long clientId);

    List<Defect> findByApplicationId(Long applicationId);

    List<Defect> findByTestPlanId(Long testPlanId);

    List<Defect> findByAssignedToId(Long assignedToId);

    List<Defect> findByStatus(Defect.Status status);

    List<Defect> findBySeverity(Defect.Severity severity);

    Page<Defect> findByClientId(Long clientId, Pageable pageable);

    @Query("SELECT d FROM Defect d WHERE d.client.id = :clientId AND " +
           "(LOWER(d.defectId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Defect> searchDefectsByClient(@Param("clientId") Long clientId, @Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(d) FROM Defect d")
    long countTotalDefects();

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.status IN ('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST')")
    long countOpenDefects();

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.status = 'CLOSED'")
    long countClosedDefects();

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.severity = :severity")
    long countBySeverity(@Param("severity") Defect.Severity severity);

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.status = :status")
    long countByStatus(@Param("status") Defect.Status status);

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.application.id = :applicationId")
    long countByApplication(@Param("applicationId") Long applicationId);

    Optional<Defect> findByDefectId(String defectId);

    @Query("SELECT d FROM Defect d WHERE d.createdAt BETWEEN :startDate AND :endDate")
    List<Defect> findDefectsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT MAX(CAST(SUBSTRING(d.defectId, 5) AS long)) FROM Defect d")
    Long findMaxDefectIdNumber();
}