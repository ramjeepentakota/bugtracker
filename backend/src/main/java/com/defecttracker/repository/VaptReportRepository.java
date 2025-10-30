package com.defecttracker.repository;

import com.defecttracker.entity.VaptReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaptReportRepository extends JpaRepository<VaptReport, Long> {

    @Query("SELECT vr FROM VaptReport vr WHERE vr.client.id = :clientId AND vr.application.id = :applicationId")
    Optional<VaptReport> findByClientAndApplication(@Param("clientId") Long clientId, @Param("applicationId") Long applicationId);
}
