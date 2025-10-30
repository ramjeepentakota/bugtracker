package com.defecttracker.controller;

import com.defecttracker.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/defects-by-application")
    public ResponseEntity<Map<String, Object>> getDefectsByApplication() {
        Map<String, Object> data = dashboardService.getDefectsByApplication();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<Map<String, Object>> getMonthlyTrends() {
        Map<String, Object> data = dashboardService.getMonthlyTrends();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/clients-most-defects")
    public ResponseEntity<Map<String, Object>> getClientsWithMostDefects() {
        Map<String, Object> data = dashboardService.getClientsWithMostDefects();
        return ResponseEntity.ok(data);
    }
}
