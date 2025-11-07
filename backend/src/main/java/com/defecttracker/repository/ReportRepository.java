package com.defecttracker.repository;

import com.defecttracker.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r ORDER BY r.generatedAt DESC")
    List<Report> findAllOrderByGeneratedAtDesc();

    List<Report> findByReportType(Report.ReportType reportType);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.reportType = :reportType")
    long countByReportType(@Param("reportType") Report.ReportType reportType);
}
