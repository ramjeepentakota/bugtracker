package com.defecttracker.service;

import com.defecttracker.entity.Defect;
import com.defecttracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private TestPlanRepository testPlanRepository;

    @Autowired
    private DefectRepository defectRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalClients", clientRepository.countTotalClients());
        stats.put("totalApplications", applicationRepository.countTotalApplications());
        stats.put("totalTestPlans", testPlanRepository.countTotalTestPlans());
        stats.put("totalDefects", defectRepository.countTotalDefects());
        stats.put("openDefects", defectRepository.countOpenDefects());
        stats.put("closedDefects", defectRepository.countClosedDefects());

        // Defects by severity
        Map<String, Long> defectsBySeverity = new HashMap<>();
        for (Defect.Severity severity : Defect.Severity.values()) {
            defectsBySeverity.put(severity.name(), defectRepository.countBySeverity(severity));
        }
        stats.put("defectsBySeverity", defectsBySeverity);

        // Defects by status
        Map<String, Long> defectsByStatus = new HashMap<>();
        for (Defect.Status status : Defect.Status.values()) {
            defectsByStatus.put(status.name(), defectRepository.countByStatus(status));
        }
        stats.put("defectsByStatus", defectsByStatus);

        return stats;
    }

    public Map<String, Object> getDefectsByApplication() {
        List<Object[]> results = defectRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                defect -> defect.getApplication().getApplicationName(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
            .collect(Collectors.toList());

        return Map.of("data", results);
    }

    public Map<String, Object> getMonthlyTrends() {
        // Get defects from last 6 months
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Defect> recentDefects = defectRepository.findDefectsByDateRange(sixMonthsAgo, LocalDateTime.now());

        // Group by month
        Map<String, Long> monthlyData = recentDefects.stream()
            .collect(Collectors.groupingBy(
                defect -> defect.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.counting()
            ));

        List<Map<String, Object>> data = monthlyData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> map = new HashMap<>();
                map.put("month", entry.getKey());
                map.put("count", entry.getValue());
                return map;
            })
            .collect(Collectors.toList());

        return Map.of("data", data);
    }

    public Map<String, Object> getClientsWithMostDefects() {
        List<Object[]> results = clientRepository.findAll().stream()
            .map(client -> {
                long defectCount = defectRepository.countByApplication(client.getId());
                return new Object[]{client.getClientName(), defectCount};
            })
            .sorted((a, b) -> Long.compare((Long) b[1], (Long) a[1]))
            .limit(10)
            .collect(Collectors.toList());

        return Map.of("data", results);
    }
}