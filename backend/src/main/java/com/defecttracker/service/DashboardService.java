package com.defecttracker.service;

import com.defecttracker.entity.Defect;
import com.defecttracker.entity.VaptTestCase;
import com.defecttracker.entity.TestPlan;
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

    @Autowired
    private VaptTestCaseRepository vaptTestCaseRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalClients", clientRepository.countTotalClients());
        stats.put("totalApplications", applicationRepository.countTotalApplications());
        stats.put("totalTestPlans", testPlanRepository.countTotalTestPlans());
        stats.put("totalDefects", defectRepository.countTotalDefects());
        stats.put("openDefects", defectRepository.countOpenDefects());
        stats.put("closedDefects", defectRepository.countClosedDefects());

        // VAPT Test Case vulnerability status counts
        stats.put("openVulnerabilities", vaptTestCaseRepository.countByVulnerabilityStatus(VaptTestCase.VulnerabilityStatus.OPEN));
        stats.put("closedVulnerabilities", vaptTestCaseRepository.countByVulnerabilityStatus(VaptTestCase.VulnerabilityStatus.CLOSED));

        // VAPT test cases by severity (using TestPlan.Severity)
        Map<String, Long> defectsBySeverity = new HashMap<>();
        for (TestPlan.Severity severity : TestPlan.Severity.values()) {
            defectsBySeverity.put(severity.name(), vaptTestCaseRepository.countBySeverity(severity));
        }
        stats.put("defectsBySeverity", defectsBySeverity);


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

        Map<String, Object> response = new HashMap<>();
        response.put("data", results);
        return response;
    }

    public Map<String, Object> getMonthlyTrends() {
        // Get VAPT test cases from last 6 months
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<VaptTestCase> recentVaptCases = vaptTestCaseRepository.findByCreatedAtBetween(sixMonthsAgo, LocalDateTime.now());

        // Group by month and status
        Map<String, Map<String, Long>> monthlyData = recentVaptCases.stream()
            .collect(Collectors.groupingBy(
                vaptCase -> vaptCase.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.groupingBy(
                    vaptCase -> vaptCase.getVulnerabilityStatus().name(),
                    Collectors.counting()
                )
            ));

        List<Map<String, Object>> data = monthlyData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> map = new HashMap<>();
                map.put("month", entry.getKey());
                Map<String, Long> statusCounts = entry.getValue();
                map.put("count", statusCounts.getOrDefault("OPEN", 0L) + statusCounts.getOrDefault("CLOSED", 0L));
                return map;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        return response;
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

        Map<String, Object> response = new HashMap<>();
        response.put("data", results);
        return response;
    }
}
