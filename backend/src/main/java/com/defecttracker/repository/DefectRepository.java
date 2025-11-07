package com.defecttracker.repository;

import com.defecttracker.entity.Defect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefectRepository extends JpaRepository<Defect, Long> {

    List<Defect> findByClientId(Long clientId);

    @Query("SELECT d FROM Defect d WHERE d.client.id = :clientId AND " +
           "(LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.defectId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Defect> searchDefectsByClient(@Param("clientId") Long clientId, @Param("search") String search, Pageable pageable);

    Optional<Defect> findByDefectId(String defectId);

    @Query("SELECT COUNT(d) FROM Defect d")
    long countTotalDefects();

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.status IN ('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST')")
    long countOpenDefects();

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.status = 'CLOSED'")
    long countClosedDefects();

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.application.id = :applicationId")
    long countByApplication(@Param("applicationId") Long applicationId);

    @Query("SELECT MAX(CAST(SUBSTRING(d.defectId, 6) AS long)) FROM Defect d")
    Long findMaxDefectIdNumber();
}
