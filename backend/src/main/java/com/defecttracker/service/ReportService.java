package com.defecttracker.service;

import com.defecttracker.entity.Report;
import com.defecttracker.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    public List<Report> getAllReports() {
        return reportRepository.findAllOrderByGeneratedAtDesc();
    }

    public List<Report> getReportsByType(Report.ReportType reportType) {
        return reportRepository.findByReportType(reportType);
    }

    public Report createReport(Report report) {
        return reportRepository.save(report);
    }

    public long countReportsByType(Report.ReportType reportType) {
        return reportRepository.countByReportType(reportType);
    }
}
