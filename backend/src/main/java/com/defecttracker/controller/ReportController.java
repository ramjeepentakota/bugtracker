package com.defecttracker.controller;

import com.defecttracker.entity.Report;
import com.defecttracker.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        List<Report> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/type/{reportType}")
    public ResponseEntity<List<Report>> getReportsByType(@PathVariable Report.ReportType reportType) {
        List<Report> reports = reportService.getReportsByType(reportType);
        return ResponseEntity.ok(reports);
    }

    @PostMapping
    public ResponseEntity<Report> createReport(@RequestBody Report report) {
        Report createdReport = reportService.createReport(report);
        return ResponseEntity.ok(createdReport);
    }

    @GetMapping("/count/{reportType}")
    public ResponseEntity<Long> countReportsByType(@PathVariable Report.ReportType reportType) {
        long count = reportService.countReportsByType(reportType);
        return ResponseEntity.ok(count);
    }
}
