
package com.defecttracker.service;

import com.defecttracker.entity.*;
import com.defecttracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.xwpf.usermodel.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.ChartUtils;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@Service
public class VaptReportService {

    @Autowired
    private VaptReportRepository vaptReportRepository;

    @Autowired
    private VaptTestCaseRepository vaptTestCaseRepository;

    @Autowired
    private VaptPocRepository vaptPocRepository;

    @Autowired
    private TestPlanRepository testPlanRepository;

    public VaptTestCaseRepository getVaptTestCaseRepository() {
        return vaptTestCaseRepository;
    }

    public TestPlanRepository getTestPlanRepository() {
        return testPlanRepository;
    }

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ApplicationRepository applicationRepository;


    @Transactional
    public VaptReport getOrCreateVaptReport(Long clientId, Long applicationId, List<Long> selectedTestPlanIds, User currentUser) {
        Optional<VaptReport> existingReport = vaptReportRepository.findByClientAndApplication(clientId, applicationId);

        if (existingReport.isPresent()) {
            return existingReport.get();
        }

        // Create new report
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        VaptReport vaptReport = new VaptReport();
        vaptReport.setClient(client);
        vaptReport.setApplication(application);
        vaptReport.setCreatedBy(currentUser);
        vaptReport.setReportName("VAPT Report - " + client.getClientName() + " - " + application.getApplicationName());
        vaptReport.setStatus(VaptReport.Status.INITIALIZED);

        // Initialize new fields with default values
        vaptReport.setAssessmentDate(LocalDate.now());
        vaptReport.setReportVersion("1.0." + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        vaptReport.setPreparedBy(currentUser.getUsername());
        vaptReport.setReviewedBy("Security Team Lead");
        vaptReport.setSubmittedTo(client.getClientName());
        vaptReport.setObjective("Comprehensive Vulnerability Assessment and Penetration Testing (VAPT) of " + application.getApplicationName() + ".");
        vaptReport.setScope(application.getApplicationName() + " (" + application.getEnvironment().toString() + ")");
        vaptReport.setApproach("Black Box Testing Methodology");
        vaptReport.setKeyHighlights("Automated report with charts and summaries");
        vaptReport.setOverallRisk("Minimal Risk");
        vaptReport.setAssetType("Web Application");
        vaptReport.setUrls(application.getApplicationName());
        vaptReport.setStartDate(LocalDate.now());
        vaptReport.setEndDate(LocalDate.now());
        vaptReport.setTotalDays(1L);
        vaptReport.setTesters(currentUser.getUsername());
        vaptReport.setCriticalCount(0L);
        vaptReport.setHighCount(0L);
        vaptReport.setMediumCount(0L);
        vaptReport.setLowCount(0L);
        vaptReport.setInfoCount(0L);
        vaptReport.setPostureLevel("Minimal Risk");
        vaptReport.setRecommendations("Implement proper input validation and sanitization. Regular security assessments and penetration testing. Keep all software and dependencies updated. Implement security headers and best practices. Conduct regular security awareness training.");
        vaptReport.setNextSteps("Remediate identified vulnerabilities. Conduct retesting. Monitor continuously.");
        vaptReport.setApprovedBy(client.getClientName());

        VaptReport savedReport = vaptReportRepository.save(vaptReport);

        // Create VaptTestCase entries only for selected test plans
        if (selectedTestPlanIds != null && !selectedTestPlanIds.isEmpty()) {
            List<TestPlan> selectedTestPlans = testPlanRepository.findAllById(selectedTestPlanIds);

            System.out.println("Found " + selectedTestPlans.size() + " test plans for IDs: " + selectedTestPlanIds);

            if (selectedTestPlans.isEmpty()) {
                throw new RuntimeException("No test plans found for the selected IDs: " + selectedTestPlanIds);
            }

            for (TestPlan testPlan : selectedTestPlans) {
                System.out.println("Creating test case for test plan: " + testPlan.getId() + " - " + testPlan.getVulnerabilityName());
                VaptTestCase vaptTestCase = new VaptTestCase();
                vaptTestCase.setVaptReport(savedReport);
                vaptTestCase.setTestPlan(testPlan);
                vaptTestCase.setStatus(VaptTestCase.Status.FAILED); // OPEN vulnerability status means FAILED status
                vaptTestCase.setDescription(testPlan.getDescription());
                vaptTestCase.setTestProcedure(testPlan.getTestProcedure());
                vaptTestCase.setVulnerabilityStatus(VaptTestCase.VulnerabilityStatus.OPEN);
                vaptTestCase.setUpdatedBy(null);
                vaptTestCaseRepository.save(vaptTestCase);
                System.out.println("Created test case with ID: " + vaptTestCase.getId());
            }
        } else {
            System.out.println("No test plan IDs provided for report creation");
        }

        // Force flush to ensure all test cases are committed before returning
        vaptTestCaseRepository.flush();

        return savedReport;
    }

    public List<VaptTestCase> getVaptTestCases(Long vaptReportId) {
        return vaptTestCaseRepository.findByVaptReportIdOrdered(vaptReportId);
    }

    @Transactional
    public VaptTestCase updateVaptTestCase(Long testCaseId, VaptTestCase.Status status, String findings, String description, String testProcedure, TestPlan.Severity severity, VaptTestCase.VulnerabilityStatus vulnerabilityStatus, User currentUser) {
        VaptTestCase vaptTestCase = vaptTestCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("VAPT test case not found"));

        vaptTestCase.setFindings(findings);
        // Always use description and testProcedure from test plan exactly
        vaptTestCase.setDescription(vaptTestCase.getTestPlan().getDescription());
        vaptTestCase.setTestProcedure(vaptTestCase.getTestPlan().getTestProcedure());
        vaptTestCase.setSeverity(severity);
        vaptTestCase.setVulnerabilityStatus(vulnerabilityStatus);

        // Set status based on vulnerability status - backend determines this, not frontend
        if (vulnerabilityStatus == VaptTestCase.VulnerabilityStatus.CLOSED) {
            vaptTestCase.setStatus(VaptTestCase.Status.PASSED);
        } else if (vulnerabilityStatus == VaptTestCase.VulnerabilityStatus.OPEN) {
            vaptTestCase.setStatus(VaptTestCase.Status.FAILED);
        } else {
            // Default fallback
            vaptTestCase.setStatus(VaptTestCase.Status.NOT_STARTED);
        }
        vaptTestCase.setUpdatedBy(currentUser);

        VaptTestCase savedTestCase = vaptTestCaseRepository.save(vaptTestCase);

        // Update report counts after test case update - ensure immediate consistency
        updateReportCounts(vaptTestCase.getVaptReport().getId());

        return savedTestCase;
    }

    @Transactional
    public VaptPoc addPoc(Long testCaseId, String fileName, String filePath, String description, String evidences, User currentUser) {
        VaptTestCase vaptTestCase = vaptTestCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("VAPT test case not found"));

        VaptPoc poc = new VaptPoc();
        poc.setVaptTestCase(vaptTestCase);
        poc.setFileName(fileName);
        poc.setFilePath(filePath);
        poc.setDescription(description);
        poc.setEvidences(evidences);
        poc.setUploadedBy(currentUser);

        return vaptPocRepository.save(poc);
    }

    public List<VaptPoc> getPocs(Long testCaseId) {
        return vaptPocRepository.findByVaptTestCaseId(testCaseId);
    }

    @Transactional
    public VaptPoc updatePoc(Long pocId, String description, User currentUser) {
        VaptPoc poc = vaptPocRepository.findById(pocId)
                .orElseThrow(() -> new RuntimeException("PoC not found"));

        poc.setDescription(description);

        return vaptPocRepository.save(poc);
    }

    @Transactional
    public void deletePoc(Long pocId, User currentUser) {
        VaptPoc poc = vaptPocRepository.findById(pocId)
                .orElseThrow(() -> new RuntimeException("PoC not found"));

        // Delete the physical file if it exists
        if (poc.getFilePath() != null) {
            try {
                java.nio.file.Path filePath = java.nio.file.Paths.get(poc.getFilePath());
                java.nio.file.Files.deleteIfExists(filePath);
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Failed to delete file: " + poc.getFilePath());
            }
        }

        vaptPocRepository.delete(poc);
    }

    public Optional<VaptReport> getVaptReportById(Long id) {
        return vaptReportRepository.findById(id);
    }

    public String generateDocxReport(Long reportId) throws IOException {
        VaptReport report = getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));
        List<VaptTestCase> testCases = getVaptTestCases(reportId);

        // Filter testCases to only OPEN vulnerabilities for summary counts
        List<VaptTestCase> openTestCases = testCases.stream()
                .filter(tc -> tc.getVulnerabilityStatus() == VaptTestCase.VulnerabilityStatus.OPEN)
                .collect(Collectors.toList());

        Map<String, Long> severityCounts = openTestCases.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getTestPlan().getSeverity().toString(),
                        Collectors.counting()
                ));
        long totalVulnerabilities = severityCounts.values().stream().mapToLong(Long::longValue).sum();

        // Charts from HTML generator
        String riskDistributionChart = generateRiskDistributionChart(severityCounts);
        String vulnerableAssetsChart = generateVulnerableAssetsChart(testCases);
        String topApplicationsChart = generateTopApplicationsChart();

        // Reports directory
        String reportsDir = "/app/reports/";
        Path reportsPath = Paths.get(reportsDir);
        if (!Files.exists(reportsPath)) Files.createDirectories(reportsPath);
        String fileName = "vapt-report-" + reportId + ".docx";
        String filePath = reportsDir + fileName;

        try (XWPFDocument document = new XWPFDocument()) {
                // Cover Page - Matching HTML design with colors
                XWPFParagraph coverTitle = document.createParagraph();
                coverTitle.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun coverRun = coverTitle.createRun();
                coverRun.setBold(true); coverRun.setFontSize(24);
                coverRun.setColor("00c6ff"); // Cyber blue
                coverRun.setText("DEFECTRIX");
                coverRun.addBreak();
                XWPFRun subtitleRun = coverTitle.createRun();
                subtitleRun.setFontSize(18);
                subtitleRun.setColor("0072ff");
                subtitleRun.setText("Cybersecurity Intelligence Platform");
                subtitleRun.addBreak();
                XWPFRun reportTitleRun = coverTitle.createRun();
                reportTitleRun.setBold(true); reportTitleRun.setFontSize(20);
                reportTitleRun.setColor("00ff8f"); // Cyber green
                reportTitleRun.setText("VAPT Assessment Report");
                reportTitleRun.addBreak();

                // Client and Application info with styling
                XWPFParagraph clientInfo = document.createParagraph();
                clientInfo.setAlignment(ParagraphAlignment.LEFT);
                clientInfo.setSpacingBefore(400);
                XWPFRun clientRun = clientInfo.createRun();
                clientRun.setText("Client: ");
                clientRun.setColor("c9d1d9"); // Light gray
                clientRun.setBold(true);
                XWPFRun clientValueRun = clientInfo.createRun();
                clientValueRun.setText(report.getClient().getClientName());
                clientValueRun.setColor("58a6ff"); // Blue
                clientValueRun.setFontFamily("Roboto Mono");
                clientRun.addBreak();
                clientRun.setText("Application: ");
                clientValueRun = clientInfo.createRun();
                clientValueRun.setText(report.getApplication().getApplicationName());
                clientValueRun.setColor("58a6ff");
                clientValueRun.setFontFamily("Roboto Mono");
                clientRun.addBreak();
                clientRun.setText("Date: ");
                clientValueRun = clientInfo.createRun();
                clientValueRun.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                clientValueRun.setColor("58a6ff");
                clientValueRun.setFontFamily("Roboto Mono");
                clientRun.addBreak();

                // Document Attributes with table styling
                XWPFParagraph attrsTitle = document.createParagraph();
                attrsTitle.setSpacingBefore(400);
                XWPFRun at = attrsTitle.createRun(); at.setBold(true); at.setFontSize(16);
                at.setColor("00ff8f"); // Green
                at.setText("Document Attributes");
                XWPFTable attrs = document.createTable(3, 4);


                setRow(attrs.getRow(0), new String[]{"Date of Assessment", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), "Version", getVersion()});
                setRow(attrs.getRow(1), new String[]{"Prepared by", report.getCreatedBy().getUsername(), "Reviewed by", "Security Team Lead"});
                setRow(attrs.getRow(2), new String[]{"Submitted to", report.getClient().getClientName(), "", ""});

                // Executive Summary with colors
                XWPFParagraph exTitle = document.createParagraph();
                exTitle.setSpacingBefore(400);
                XWPFRun exR = exTitle.createRun(); exR.setBold(true); exR.setFontSize(16);
                exR.setColor("00c6ff"); // Blue
                exR.setText("Executive Summary");
                XWPFParagraph exBody = document.createParagraph();
                XWPFRun ex = exBody.createRun();
                ex.setText("Objective: ");
                ex.setBold(true);
                ex.setColor("c9d1d9");
                XWPFRun exValue = exBody.createRun();
                exValue.setText((report.getObjective() != null ? report.getObjective() : "Comprehensive VAPT assessment"));
                exValue.setColor("58a6ff");
                ex.addBreak();
                ex.setText("Scope: ");
                exValue = exBody.createRun();
                exValue.setText((report.getScope() != null ? report.getScope() : report.getApplication().getApplicationName() + " (" + report.getApplication().getEnvironment().toString() + ")"));
                exValue.setColor("58a6ff");
                ex.addBreak();
                ex.setText("Approach: ");
                exValue = exBody.createRun();
                exValue.setText((report.getApproach() != null ? report.getApproach() : "Black Box Testing Methodology"));
                exValue.setColor("58a6ff");
                ex.addBreak();
                ex.setText("Overall Risk Posture: ");
                exValue = exBody.createRun();
                exValue.setText(calculateOverallRisk(severityCounts));
                exValue.setBold(true);
                exValue.setColor("f85149"); // Red for risk
                ex.addBreak();

                // VAPT Test Graphs with styled titles
                XWPFParagraph graphsTitle = document.createParagraph();
                graphsTitle.setSpacingBefore(400);
                XWPFRun gtr = graphsTitle.createRun(); gtr.setBold(true); gtr.setFontSize(16);
                gtr.setColor("00ff8f"); // Green
                gtr.setText("VAPT Test Graphs");
                insertBase64Png(document, riskDistributionChart);
                insertBase64Png(document, vulnerableAssetsChart);
                insertBase64Png(document, topApplicationsChart);

                // Auditing Scope
                XWPFParagraph scopeTitle = document.createParagraph(); scopeTitle.setSpacingBefore(400);
                XWPFRun sc = scopeTitle.createRun(); sc.setBold(true); sc.setFontSize(16);
                sc.setColor("00c6ff");
                sc.setText("Auditing Scope");
                XWPFTable scope = document.createTable(4, 2);
                setRow(scope.getRow(0), new String[]{"Client Name", report.getClient().getClientName()});
                setRow(scope.getRow(1), new String[]{"Application Name", report.getApplication().getApplicationName()});
                setRow(scope.getRow(2), new String[]{"Asset Type", report.getAssetType() != null ? report.getAssetType() : "Web Application"});
                setRow(scope.getRow(3), new String[]{"Environment", report.getApplication().getEnvironment().toString()});

                // Project Timeframe
                XWPFParagraph tfTitle = document.createParagraph(); tfTitle.setSpacingBefore(400);
                XWPFRun tfr = tfTitle.createRun(); tfr.setBold(true); tfr.setFontSize(16);
                tfr.setColor("00c6ff");
                tfr.setText("Project Timeframe");
                XWPFTable tf = document.createTable(4, 2);
                setRow(tf.getRow(0), new String[]{"Start Date", report.getStartDate() != null ? report.getStartDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : (report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A")});
                setRow(tf.getRow(1), new String[]{"End Date", report.getEndDate() != null ? report.getEndDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))});
                setRow(tf.getRow(2), new String[]{"Total Days", String.valueOf(report.getTotalDays() != null ? report.getTotalDays() : calculateTestingDays(report.getCreatedAt()))});
                setRow(tf.getRow(3), new String[]{"Testers", report.getTesters() != null ? report.getTesters() : report.getCreatedBy().getUsername()});

                // Methodologies & Standards
                XWPFParagraph methTitle = document.createParagraph(); methTitle.setSpacingBefore(400);
                XWPFRun mr = methTitle.createRun(); mr.setBold(true); mr.setFontSize(16);
                mr.setColor("00ff8f");
                mr.setText("Methodologies & Standards");
                XWPFParagraph ml = document.createParagraph();
                XWPFRun mlr = ml.createRun();
                mlr.setColor("58a6ff");
                mlr.setText("• OWASP Testing Guide v4.2");
                mlr.addBreak();
                mlr.setText("• PTES");
                mlr.addBreak();
                mlr.setText("• OSSTMM");
                mlr.addBreak();
                mlr.setText("• WSTG");
                mlr.addBreak();
                mlr.setText("• NIST SP 800-115");
                mlr.addBreak();

                // Risk Ratings and Threat Level with colored severity badges
                XWPFParagraph rrTitle = document.createParagraph(); rrTitle.setSpacingBefore(400);
                XWPFRun rrr = rrTitle.createRun(); rrr.setBold(true); rrr.setFontSize(16);
                rrr.setColor("f85149"); // Red
                rrr.setText("Risk Ratings and Threat Level");
                XWPFTable rr = document.createTable(5, 2);
                setRow(rr.getRow(0), new String[]{"Critical", "Immediate, direct, and demonstrable impact to business, data, or systems. (e.g., RCE, Full Auth Bypass)"});
                setRow(rr.getRow(1), new String[]{"High", "Significant impact on confidentiality, integrity, or availability. (e.g., SQLi, Stored XSS, IDOR)"});
                setRow(rr.getRow(2), new String[]{"Medium", "Potential to impact business, but requires more complex exploitation. (e.g., Reflected XSS, CSRF)"});
                setRow(rr.getRow(3), new String[]{"Low", "Minor security weakness with low or limited impact. (e.g., Info Disclosure, Weak SSL Ciphers)"});
                setRow(rr.getRow(4), new String[]{"Info", "Observational finding or security best practice recommendation."});

                // Vulnerability Summary with colored counts
                XWPFParagraph vsTitle = document.createParagraph(); vsTitle.setSpacingBefore(400);
                XWPFRun vsr = vsTitle.createRun(); vsr.setBold(true); vsr.setFontSize(16);
                vsr.setColor("f08833"); // Orange
                vsr.setText("Vulnerability Summary");
                XWPFTable vs = document.createTable(6, 2);
                setRow(vs.getRow(0), new String[]{"Critical", String.valueOf(severityCounts.getOrDefault("CRITICAL", 0L))});
                setRow(vs.getRow(1), new String[]{"High", String.valueOf(severityCounts.getOrDefault("HIGH", 0L))});
                setRow(vs.getRow(2), new String[]{"Medium", String.valueOf(severityCounts.getOrDefault("MEDIUM", 0L))});
                setRow(vs.getRow(3), new String[]{"Low", String.valueOf(severityCounts.getOrDefault("LOW", 0L))});
                setRow(vs.getRow(4), new String[]{"Info", String.valueOf(severityCounts.getOrDefault("INFO", 0L))});
                setRow(vs.getRow(5), new String[]{"Total", String.valueOf(totalVulnerabilities)});
                insertBase64Png(document, riskDistributionChart);

                // Test Case Details table - Show ALL test cases (OPEN and CLOSED)
                XWPFParagraph tcTitle = document.createParagraph(); tcTitle.setSpacingBefore(400);
                XWPFRun tcr = tcTitle.createRun(); tcr.setBold(true); tcr.setFontSize(16);
                tcr.setColor("dbab09"); // Yellow
                tcr.setText("Test Case Details");
        
                XWPFTable tcTable = document.createTable(testCases.size() + 1, 8);
                // Set headers
                XWPFTableRow headerRow = tcTable.getRow(0);
                headerRow.getCell(0).setText("Test Case ID");
                headerRow.getCell(1).setText("Vulnerability Name");
                headerRow.getCell(2).setText("Severity");
                headerRow.getCell(3).setText("Status");
                headerRow.getCell(4).setText("Description");
                headerRow.getCell(5).setText("Test Procedure");
                headerRow.getCell(6).setText("Findings");
                headerRow.getCell(7).setText("Vulnerability Status");
        
                // Add data rows - Show ALL test cases
                for (int i = 0; i < testCases.size(); i++) {
                    VaptTestCase testCase = testCases.get(i);
                    XWPFTableRow row = tcTable.getRow(i + 1);
                    row.getCell(0).setText(testCase.getTestPlan().getTestCaseId());
                    row.getCell(1).setText(testCase.getTestPlan().getVulnerabilityName());
                    row.getCell(2).setText(testCase.getTestPlan().getSeverity().toString());
                    row.getCell(3).setText(getStatusDisplay(testCase.getStatus()));
                    row.getCell(4).setText(testCase.getTestPlan().getDescription());
                    row.getCell(5).setText(testCase.getTestPlan().getTestProcedure());
                    row.getCell(6).setText(testCase.getFindings() != null ? testCase.getFindings() : "N/A");
                    row.getCell(7).setText(testCase.getVulnerabilityStatus().toString());
                }

                // Detailed Observations with severity-based coloring - Show ALL test cases
                XWPFParagraph obTitle = document.createParagraph(); obTitle.setSpacingBefore(400);
                XWPFRun obr = obTitle.createRun(); obr.setBold(true); obr.setFontSize(16);
                obr.setColor("dbab09"); // Yellow
                obr.setText("Detailed Observations");
                for (VaptTestCase testCase : testCases) {
                    XWPFParagraph cTitle = document.createParagraph();
                    cTitle.setSpacingBefore(200);
                    XWPFRun crun = cTitle.createRun(); crun.setBold(true); crun.setFontSize(14);
                    // Color based on severity
                    String severityColor = getSeverityColor(testCase.getTestPlan().getSeverity().toString());
                    crun.setColor(severityColor);
                    String statusText = testCase.getStatus() != null ? " [" + testCase.getStatus().toString() + "]" : " [NOT_STARTED]";
                    crun.setText("Bug: " + testCase.getTestPlan().getVulnerabilityName() + " (" + testCase.getTestPlan().getSeverity() + ")" + statusText);

                    XWPFParagraph cBody = document.createParagraph();
                    XWPFRun cb = cBody.createRun();
                    cb.setText("Affected URL / Asset: ");
                    cb.setColor("c9d1d9");
                    XWPFRun cbValue = cBody.createRun();
                    cbValue.setText(report.getApplication().getApplicationName());
                    cbValue.setColor("58a6ff");
                    cbValue.setFontFamily("Roboto Mono");
                    cb.addBreak();
                    cb.setText("Description: ");
                    cbValue = cBody.createRun();
                    cbValue.setText(testCase.getTestPlan().getDescription());
                    cbValue.setColor("c9d1d9");
                    cb.addBreak();
                    cb.setText("Attack Procedure: ");
                    cbValue = cBody.createRun();
                    cbValue.setText(testCase.getTestPlan().getTestProcedure());
                    cbValue.setColor("c9d1d9");
                    cb.addBreak();
                    cb.setText("Impact: ");
                    cbValue = cBody.createRun();
                    cbValue.setText(generateImpactDescription(testCase.getTestPlan().getSeverity()));
                    cbValue.setColor("f85149");
                    cb.addBreak();

                    List<VaptPoc> pocs = getPocs(testCase.getId());
                    if (!pocs.isEmpty()) {
                        cb.setText("Proof of Concepts:");
                        cb.setColor("00c6ff");
                        cb.addBreak();
                        for (VaptPoc poc : pocs) {
                            cb.setText("• ");
                            cbValue = cBody.createRun();
                            cbValue.setText(poc.getFileName() + (poc.getDescription() != null && !poc.getDescription().trim().isEmpty() ? " (" + poc.getDescription() + ")" : ""));
                            cbValue.setColor("58a6ff");
                            cb.addBreak();
                            // Add image if file exists
                            if (poc.getFilePath() != null && !poc.getFilePath().trim().isEmpty()) {
                                try {
                                    Path pocFilePath = Paths.get(poc.getFilePath());
                                    if (Files.exists(pocFilePath)) {
                                        XWPFParagraph imgPara = document.createParagraph();
                                        XWPFRun imgRun = imgPara.createRun();
                                        try (InputStream is = new FileInputStream(poc.getFilePath())) {
                                            String mimeType = getMimeType(poc.getFileName());
                                            if (mimeType.startsWith("image/")) {
                                                imgRun.addPicture(is, getPictureType(mimeType), poc.getFileName(), org.apache.poi.util.Units.toEMU(300), org.apache.poi.util.Units.toEMU(200));
                                            }
                                        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
                                            // Skip if not a supported image format
                                        }
                                    }
                                } catch (Exception e) {
                                    // Skip if file cannot be loaded
                                }
                            }
                        }
                    }
                }

                // Tools Used with styled icons
                XWPFParagraph toolsTitle = document.createParagraph(); toolsTitle.setSpacingBefore(400);
                XWPFRun tlr = toolsTitle.createRun(); tlr.setBold(true); tlr.setFontSize(16);
                tlr.setColor("00c6ff");
                tlr.setText("Tools Used for the Assessment");
                XWPFParagraph tl = document.createParagraph(); XWPFRun tlrun = tl.createRun();
                tlrun.setColor("00ff8f");
                for (String tool : new String[]{"Burp Suite", "Nmap", "Nessus", "OWASP ZAP", "Amass/Subfinder", "Dirsearch/Gobuster", "Custom Scripts"}) {
                    tlrun.setText("• " + tool); tlrun.addBreak();
                }
                XWPFParagraph tln = document.createParagraph(); tln.createRun().setText("All tools used in combination with manual verification and exploitation techniques.");

                // Conclusion & Recommendations with styled sections
                XWPFParagraph conTitle = document.createParagraph(); conTitle.setSpacingBefore(400);
                XWPFRun conr = conTitle.createRun(); conr.setBold(true); conr.setFontSize(16);
                conr.setColor("00ff8f");
                conr.setText("Conclusion & Recommendations");
                XWPFParagraph con = document.createParagraph(); XWPFRun conb = con.createRun();
                conb.setText("Overall application security posture: ");
                conb.setColor("c9d1d9");
                XWPFRun conValue = con.createRun();
                conValue.setText(calculateOverallRisk(severityCounts));
                conValue.setBold(true);
                conValue.setColor("f85149");
                conb.addBreak();
                conb.setText("Key Recommendations:");
                conb.setColor("00c6ff");
                conb.setBold(true);
                conb.addBreak();
                String[] recommendations = {
                    "Implement proper input validation and sanitization",
                    "Regular security assessments and penetration testing",
                    "Keep all software and dependencies updated",
                    "Implement security headers and best practices",
                    "Conduct regular security awareness training"
                };
                for (String rec : recommendations) {
                    conb.setText("• ");
                    conValue = con.createRun();
                    conValue.setText(rec);
                    conValue.setColor("58a6ff");
                    conb.addBreak();
                }
                conb.setText("Next Steps: ");
                conValue = con.createRun();
                conValue.setText((report.getNextSteps() != null ? report.getNextSteps() : "Remediate, Retest, Monitor continuously."));
                conValue.setColor("00ff8f");
                conb.addBreak();

                // Signatures with styled boxes
                XWPFParagraph sig = document.createParagraph(); sig.setSpacingBefore(400);
                XWPFRun s = sig.createRun();
                s.setText("Prepared by: ");
                s.setColor("c9d1d9");
                XWPFRun sValue = sig.createRun();
                sValue.setText((report.getPreparedBy() != null ? report.getPreparedBy() : report.getCreatedBy().getUsername()));
                sValue.setColor("58a6ff");
                sValue.setFontFamily("Roboto Mono");
                s.addBreak();
                s.setText("Reviewed by: ");
                sValue = sig.createRun();
                sValue.setText((report.getReviewedBy() != null ? report.getReviewedBy() : "Security Team Lead"));
                sValue.setColor("58a6ff");
                sValue.setFontFamily("Roboto Mono");
                s.addBreak();
                s.setText("Approved by: ");
                sValue = sig.createRun();
                sValue.setText((report.getApprovedBy() != null ? report.getApprovedBy() : report.getClient().getClientName()));
                sValue.setColor("58a6ff");
                sValue.setFontFamily("Roboto Mono");
                s.addBreak();

                // Footer with gradient styling
                XWPFParagraph foot = document.createParagraph();
                foot.setSpacingBefore(200);
                XWPFRun fr = foot.createRun();
                fr.setText("Generated automatically by DEFECTRIX | Confidential Report | © Root Lock Defense 2025");
                fr.setColor("8b949e");
                fr.setFontSize(10);

                try (FileOutputStream out = new FileOutputStream(filePath)) {
                    document.write(out);
                }
                return "/downloads/" + fileName;
            }
        catch (Exception e) {
            throw new IOException("Failed to generate styled DOCX report: " + e.getMessage(), e);
        }
    }

    private String getSeverityColor(String severity) {
        switch (severity.toUpperCase()) {
            case "CRITICAL": return "da3633"; // Red
            case "HIGH": return "f08833"; // Orange
            case "MEDIUM": return "dbab09"; // Yellow
            case "LOW": return "2f81f7"; // Blue
            case "INFO": return "6e7681"; // Gray
            default: return "c9d1d9"; // Light gray
        }
    }

    private void setRow(XWPFTableRow row, String[] values) {
        for (int i = 0; i < values.length; i++) {
            row.getCell(i).setText(values[i]);
        }
    }

    private void insertBase64Png(XWPFDocument document, String dataUrl) throws IOException {
        if (dataUrl == null || !dataUrl.startsWith("data:image/png;base64,")) return;
        String base64 = dataUrl.substring("data:image/png;base64,".length());
        byte[] bytes = Base64.getDecoder().decode(base64);
        XWPFParagraph p = document.createParagraph();
        XWPFRun run = p.createRun();
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            run.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "chart.png", org.apache.poi.util.Units.toEMU(400), org.apache.poi.util.Units.toEMU(250));
        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
            throw new IOException(e);
        }
    }

    public String generatePdfReport(Long reportId) throws IOException {
        VaptReport report = getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));

        String htmlContent = generateHtmlReport(reportId);

        // Create reports directory if it doesn't exist
        String reportsDir = "/app/reports/";
        Path reportsPath = Paths.get(reportsDir);
        if (!Files.exists(reportsPath)) {
            Files.createDirectories(reportsPath);
        }

        String fileName = "vapt-report-" + reportId + ".pdf";
        String filePath = reportsDir + fileName;

        // Generate PDF using OpenHTMLtoPDF with full HTML support
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(fos);
            builder.run();
        } catch (Exception pdfException) {
            // Fallback to basic iText if OpenHTMLtoPDF fails
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(fos);
                com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
                com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
                document.setMargins(50, 50, 50, 50);

                document.add(new com.itextpdf.layout.element.Paragraph("DEFECTRIX - VAPT Report")
                        .setFontSize(20)
                        .setBold());
                document.add(new com.itextpdf.layout.element.Paragraph("Report ID: " + reportId));
                document.add(new com.itextpdf.layout.element.Paragraph("Client: " + report.getClient().getClientName()));
                document.add(new com.itextpdf.layout.element.Paragraph("Application: " + report.getApplication().getApplicationName()));
                document.add(new com.itextpdf.layout.element.Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));
                document.close();
            } catch (Exception fallbackException) {
                throw new IOException("Both PDF generation methods failed", fallbackException);
            }
        }

        // Verify file was created
        Path pdfPath = Paths.get(filePath);
        if (Files.exists(pdfPath)) {
            long fileSize = Files.size(pdfPath);
            if (fileSize == 0) {
                throw new IOException("Generated PDF file is empty");
            }
        } else {
            throw new IOException("PDF file was not created");
        }

        return "/downloads/" + fileName;
    }

    public String generateHtmlReportPublic(Long reportId) throws IOException {
        return generateHtmlReport(reportId);
    }

    public String generateHtmlReportFile(Long reportId) throws IOException {
        String htmlContent = generateHtmlReport(reportId);

        // Create reports directory if it doesn't exist
        String reportsDir = "/app/reports/";
        Path reportsPath = Paths.get(reportsDir);
        if (!Files.exists(reportsPath)) {
            Files.createDirectories(reportsPath);
        }

        String fileName = "vapt-report-" + reportId + ".html";
        String filePath = reportsDir + fileName;

        // Write HTML content to file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(htmlContent);
        }

        return "/downloads/" + fileName;
    }

    private String generateHtmlReport(Long reportId) throws IOException {
        VaptReport report = getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));

        List<VaptTestCase> testCases = getVaptTestCases(reportId);
        // Filter testCases to only OPEN vulnerabilities for summary counts
        List<VaptTestCase> openTestCases = testCases.stream()
                .filter(tc -> tc.getVulnerabilityStatus() == VaptTestCase.VulnerabilityStatus.OPEN)
                .collect(Collectors.toList());
        Map<String, Long> severityCounts = openTestCases.stream()
                .collect(Collectors.groupingBy(tc -> tc.getTestPlan().getSeverity().toString(), Collectors.counting()));

        long critical = severityCounts.getOrDefault("CRITICAL", 0L);
        long high = severityCounts.getOrDefault("HIGH", 0L);
        long medium = severityCounts.getOrDefault("MEDIUM", 0L);
        long low = severityCounts.getOrDefault("LOW", 0L);
        long info = severityCounts.getOrDefault("INFO", 0L);

        String clientName = report.getClient() != null ? report.getClient().getClientName() : "N/A";
        String appName = report.getApplication() != null ? report.getApplication().getApplicationName() : "N/A";
        String environment = report.getApplication() != null ? report.getApplication().getEnvironment().toString() : "N/A";
        String assessmentDate = report.getAssessmentDate() != null ? report.getAssessmentDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        String version = report.getReportVersion() != null ? report.getReportVersion() : getVersion();
        String overallRisk = report.getOverallRisk() != null ? report.getOverallRisk() : calculateOverallRisk(severityCounts);
        long totalDays = report.getTotalDays() != null ? report.getTotalDays() : calculateTestingDays(report.getCreatedAt());
        String testers = report.getTesters() != null ? report.getTesters() : (report.getCreatedBy() != null ? report.getCreatedBy().getUsername() : "N/A");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html lang=\"en\" class=\"scroll-smooth\">")
            .append("<head>")
            .append("<meta charset=\"UTF-g\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            .append("<title>DEFECTRIX - VAPT Report</title>")
            .append("<script src=\"https://cdn.tailwindcss.com\"></script>")
            .append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>")
            .append("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css\">")
            .append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\"><link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>")
            .append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&family=Poppins:wght@600;700&family=Roboto+Mono:wght@400;500&display=swap\" rel=\"stylesheet\">")
            .append("<style>")
            .append("body{font-family:'Inter',sans-serif;background-color:#0d1117;color:#c9d1d9;overflow-x:hidden;}h1,h2,h3,h4,h5,h6{font-family:'Poppins',sans-serif;font-weight:600;}code,kbd,samp,pre,.font-mono{font-family:'Roboto Mono',monospace;} .bg-gradient-cyber{background-image:linear-gradient(to right,#00c6ff,#0072ff,#00d4ff,#00ff8f);} .text-gradient-cyber{background-image:linear-gradient(to right,#00c6ff,#00ff8f);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;text-fill-color:transparent;} .border-cyber-gradient{border-image-slice:1;border-image-source:linear-gradient(to right,#0072ff,#00ff8f);} .report-card{background-color:#161b22;border:1px solid #30363d;border-radius:8px;page-break-inside:avoid;} .severity-critical{background-color:#da3633;color:#ffffff;} .severity-high{background-color:#f08833;color:#ffffff;} .severity-medium{background-color:#dbab09;color:#ffffff;} .severity-low{background-color:#2f81f7;color:#ffffff;} .severity-info{background-color:#6e7681;color:#ffffff;} .text-critical{color:#f85149;} .text-high{color:#f08833;} .text-medium{color:#dbab09;} .text-low{color:#58a6ff;} .text-info{color:#8b949e;} .main-content{background-color:#0d1117;} .tab-content{display:block;} .table-responsive{overflow-x:auto;} .grid-auto-fit{min-width:0;} @media print{.sidebar,.print-button{display:none!important;} body{background-color:#ffffff!important;color:#000000!important;font-family:'Inter',sans-serif;} .main-content{margin-left:0!important;padding:0!important;width:100%!important;border:none!important;max-width:100%!important;} .report-card,.report-section{border:1px solid #ddd!important;box-shadow:none!important;background-color:#f9f9f9!important;color:#000!important;page-break-inside:avoid!important;} h1,h2,h3,h4,h5,h6,span,p,div{color:#000000!important;} .text-gradient-cyber{background-image:none!important;-webkit-text-fill-color:#000!important;text-fill-color:#000!important;} canvas{print-color-adjust:exact;-webkit-print-color-adjust:exact;} @page{size:A4;margin:1.5cm;} a{text-decoration:none;color:#000!important;} .print-footer{display:block!important;position:fixed;bottom:0;width:100%;text-align:center;font-size:9pt;color:#888!important;} body::after{content:'CONFIDENTIAL';color:rgba(0,0,0,0.08)!important;font-size:10vw!important;} .severity-critical,.severity-high,.severity-medium,.severity-low,.severity-info{background-color:#eee!important;color:#000!important;border:1px solid #ccc;print-color-adjust:exact;-webkit-print-color-adjust:exact;} .severity-critical{background-color:#da3633!important;color:#fff!important;} .severity-high{background-color:#f08833!important;color:#fff!important;} .severity-medium{background-color:#dbab09!important;color:#fff!important;} .severity-low{background-color:#2f81f7!important;color:#fff!important;} .severity-info{background-color:#6e7681!important;color:#fff!important;}} @media (max-width:768px){.main-content{padding:1rem!important;} .report-card{margin-bottom:1rem;} .grid-cols-1,.grid-cols-2,.grid-cols-3{grid-template-columns:repeat(1,minmax(0,1fr))!important;} .table-responsive{overflow-x:scroll;} th,td{min-width:120px;white-space:nowrap;}}")
            .append("</style>")
            .append("</head>")
            .append("<body class=\"antialiased\">")
            .append("<main class=\"main-content w-full p-8\">")
            .append("<section id=\"cover-page\" class=\"report-section mb-8 min-h-[80vh] report-card flex flex-col items-center justify-center p-8 text-center\">")
            .append("<img src=\"https://placehold.co/150x150/161b22/00ff8f?text=D&font=poppins\" alt=\"DEFECTRIX Logo\" class=\"h-24 w-24 rounded-full\" />")
            .append("<h1 class=\"mt-6 text-4xl font-bold\">Cybersecurity VAPT Report</h1>")
            .append("<p class=\"mt-2 text-2xl text-gradient-cyber\">DEFECTRIX – Cybersecurity Intelligence Platform</p>")
            .append("<div class=\"mt-12 h-0.5 w-1/3 bg-gradient-cyber\"></div>")
            .append("<div class=\"mt-12 text-left\">")
            .append("<p class=\"text-lg\"><strong class=\"w-32 inline-block text-gray-400\">Client:</strong> <span class=\"font-mono\">").append(clientName).append("</span></p>")
            .append("<p class=\"text-lg\"><strong class=\"w-32 inline-block text-gray-400\">Application:</strong> <span class=\"font-mono\">").append(appName).append("</span></p>")
            .append("<p class=\"text-lg\"><strong class=\"w-32 inline-block text-gray-400\">Date:</strong> <span class=\"font-mono\">").append(assessmentDate).append("</span></p>")
            .append("</div></section>")
            .append("<section class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 border-b border-gray-700 pb-2 text-2xl font-semibold\">Document Attributes</h2>")
            .append("<div class=\"grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3\">")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Assessment Date:</span> <span class=\"font-mono\">").append(assessmentDate).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Report Version:</span> <span class=\"font-mono\">").append(version).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Prepared By:</span> <span class=\"font-mono\">").append(report.getCreatedBy().getUsername()).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Reviewed By:</span> <span class=\"font-mono\">Security Team Lead</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Submitted To:</span> <span class=\"font-mono\">").append(clientName).append("</span></div>")
            .append("</div></section>")
            .append("<section id=\"exec-summary\" class=\"report-section report-card mb-8 p-6\">")
            .append("<h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-briefcase mr-3 text-blue-400\"></i>Executive Summary</h2>")
            .append("<div class=\"grid grid-cols-1 gap-6\"> <!-- Removed lg:grid-cols-3 -->")
            .append("<div class=\"space-y-4 lg:col-span-1\"> <!-- Changed from lg:col-span-2 -->")
            .append("<h3 class=\"text-lg font-semibold text-gray-300\">Objective</h3>")
            .append("<p class=\"text-gray-400\">").append(report.getObjective() != null ? report.getObjective() : "Comprehensive VAPT assessment").append("</p>")
            .append("<h3 class=\"text-lg font-semibold text-gray-300\">Scope & Approach</h3>")
            .append("<p class=\"text-gray-400\">").append(report.getScope() != null ? report.getScope() : "").append(" ").append(report.getApproach() != null ? report.getApproach() : "").append("</p>")
            .append("<h3 class=\"text-lg font-semibold text-gray-300\">Key Highlights</h3>")
            .append("<p class=\"text-gray-400\">").append(report.getKeyHighlights() != null ? report.getKeyHighlights() : "Automated report with charts and summaries").append("</p>")
            .append("<h3 class=\"text-lg font-semibold text-gray-300\">Overall Risk Posture</h3>")
            .append("<p class=\"text-gray-400\">The application's overall risk posture is <strong class=\"font-mono text-lg text-gradient-cyber\">").append(overallRisk).append("</strong>, based on the findings and their potential impact.</p>")
            .append("</div>")
            .append("<!-- Removed Risk Posture Chart Div -->")
            .append("</div></section>")
            .append("<div class=\"grid grid-cols-1 gap-8 lg:grid-cols-2\">")
            .append("<section id=\"scope\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-crosshairs mr-3 text-blue-400\"></i>Auditing Scope</h2>")
            .append("<div class=\"space-y-3\">")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Client Name:</span> <span class=\"font-mono\">").append(clientName).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Application Name:</span> <span class=\"font-mono\">").append(appName).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Asset Type:</span> <span class=\"font-mono\">").append(report.getAssetType() != null ? report.getAssetType() : "Web Application").append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Environment:</span> <span class=\"font-mono\">").append(environment).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">URLs / IPs:</span> <pre class=\"font-mono text-sm text-gray-300\">").append(report.getUrls() != null ? report.getUrls() : appName).append("</pre></div>")
            .append("</div></section>")
            .append("<section id=\"timeframe\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-calendar-alt mr-3 text-blue-400\"></i>Project Timeframe</h2>")
            .append("<div class=\"space-y-3\">")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Start Date:</span> <span class=\"font-mono\">")
            .append(report.getStartDate() != null ? report.getStartDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : (report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A")).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">End Date:</span> <span class=\"font-mono\">").append(report.getEndDate() != null ? report.getEndDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : assessmentDate).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Total Days:</span> <span class=\"font-mono\">").append(totalDays).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Testers:</span> <span class=\"font-mono\">").append(testers).append("</span></div>")
            .append("</div></section></div>")
            .append("<section id=\"methodology\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-book mr-3 text-green-400\"></i>Methodologies & Standards</h2>")
            .append("<div class=\"grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-5\">")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-spider text-4xl text-blue-300\"></i><span class=\"mt-2 font-semibold\">OWASP</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-clipboard-list text-4xl text-blue-300\"></i><span class=\"mt-2 font-semibold\">PTES</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-shield-virus text-4xl text-blue-300\"></i><span class=\"mt-2 font-semibold\">OSSTMM</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-file-alt text-4xl text-blue-300\"></i><span class=\"mt-2 font-semibold\">WSTG</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-balance-scale text-4xl text-blue-300\"></i><span class=\"mt-2 font-semibold\">NIST SP 800-115</span></div>")
            .append("</div></section>")
            .append("<section id=\"risk-ratings\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-shield-halved mr-3 text-red-400\"></i>Risk Ratings and Threat Level</h2>")
            .append("<div class=\"table-responsive\"><table class=\"min-w-full divide-y divide-gray-700\"><thead class=\"bg-gray-800\"><tr><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Severity</th><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Description</th></tr></thead><tbody class=\"divide-y divide-gray-700\">")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-critical rounded-full px-3 py-1 text-sm font-semibold\">Critical</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Immediate, direct, and demonstrable impact to business, data, or systems. (e.g., RCE, Full Auth Bypass)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-high rounded-full px-3 py-1 text-sm font-semibold\">High</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Significant impact on confidentiality, integrity, or availability. (e.g., SQLi, Stored XSS, IDOR)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-medium rounded-full px-3 py-1 text-sm font-semibold\">Medium</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Potential to impact business, but requires more complex exploitation. (e.g., Reflected XSS, CSRF)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-low rounded-full px-3 py-1 text-sm font-semibold\">Low</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Minor security weakness with low or limited impact. (e.g., Info Disclosure, Weak SSL Ciphers)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-info rounded-full px-3 py-1 text-sm font-semibold\">Info</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Observational finding or security best practice recommendation.</td></tr>")
            .append("</tbody></table></div></div></section>")
            .append("<section id=\"vuln-summary\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-bug-slash mr-3 text-orange-400\"></i>Vulnerability Summary</h2>")
            .append("<div class=\"grid grid-cols-1 gap-6 lg:grid-cols-2\"><div><h3 class=\"mb-2 text-lg font-semibold text-gray-300\">Findings by Severity</h3><div class=\"rounded-lg border border-gray-700\"><div class=\"table-responsive\"><table class=\"min-w-full divide-y divide-gray-700\"><tbody class=\"divide-y divide-gray-700\">")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-critical\">Critical</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-critical\">").append(report.getCriticalCount() != null ? report.getCriticalCount() : critical).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-high\">High</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-high\">").append(report.getHighCount() != null ? report.getHighCount() : high).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-medium\">Medium</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-medium\">").append(report.getMediumCount() != null ? report.getMediumCount() : medium).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-low\">Low</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-low\">").append(report.getLowCount() != null ? report.getLowCount() : low).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-info\">Informational</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-info\">").append(report.getInfoCount() != null ? report.getInfoCount() : info).append("</td></tr>")
            .append("</tbody></table></div></div></div>")
            .append("<div><h3 class=\"mb-2 text-lg font-semibold text-gray-300\">Executive Scorecard (Likelihood vs. Impact)</h3>")
            .append("<div class=\"table-responsive\"><table class=\"w-full border-collapse border border-gray-700 text-center text-sm\"><thead class=\"bg-gray-800\"><tr><th class=\"border border-gray-700 p-2\" rowspan=\"2\" colspan=\"2\"></th><th class=\"border border-gray-700 p-2\" colspan=\"3\">Impact</th></tr><tr><th class=\"border border-gray-700 p-2\">Low</th><th class=\"border border-gray-700 p-2\">Medium</th><th class=\"border border-gray-700 p-2\">High</th></tr></thead><tbody><tr><th class=\"border border-gray-700 p-2\" rowspan=\"3\" valign=\"middle\">Likelihood</th><th class=\"border border-gray-700 p-2\">High</th><td class=\"severity-medium p-3\">Medium</td><td class=\"severity-high p-3\">High</td><td class=\"severity-critical p-3\">Critical</td></tr><tr><th class=\"border border-gray-700 p-2\">Medium</th><td class=\"severity-low p-3\">Low</td><td class=\"severity-medium p-3\">Medium</td><td class=\"severity-high p-3\">High</td></tr><tr><th class=\"border border-gray-700 p-2\">Low</th><td class=\"severity-info p-3\">Info</td><td class=\"severity-low p-3\">Low</td><td class=\"severity-medium p-3\">Medium</td></tr></tbody></table></div></div></div></section>")
            .append("<section id=\"test-case-details\" class=\"report-section mb-8\"><h2 class=\"mb-4 flex items-center pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-magnifying-glass mr-3 text-yellow-400\"></i>Test Case Details</h2>");

        // Create a table for Test Case Details - Show ALL test cases (OPEN and CLOSED)
        html.append("<div class=\"table-responsive\"><table class=\"min-w-full divide-y divide-gray-700\"><thead class=\"bg-gray-800\"><tr><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Test Case ID</th><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Vulnerability Name</th><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Severity</th><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Vulnerability Status</th></tr></thead><tbody class=\"divide-y divide-gray-700\">");

        for (VaptTestCase testCase : testCases) {
            String sevClass = "severity-" + testCase.getTestPlan().getSeverity().toString().toLowerCase();
            String vulnStatusText = testCase.getVulnerabilityStatus().toString();
            String vulnStatusClass = testCase.getVulnerabilityStatus() == VaptTestCase.VulnerabilityStatus.OPEN ? "text-red-400" : "text-green-400";

            html.append("<tr class=\"bg-gray-800\">")
                .append("<td class=\"whitespace-nowrap px-6 py-4 text-sm font-mono text-gray-300\">")
                .append(testCase.getTestPlan().getTestCaseId())
                .append("</td>")
                .append("<td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">")
                .append(testCase.getTestPlan().getVulnerabilityName())
                .append("</td>")
                .append("<td class=\"whitespace-nowrap px-6 py-4\"><span class=\"" + sevClass + " rounded-full px-3 py-1 text-sm font-semibold\">")
                .append(testCase.getTestPlan().getSeverity())
                .append("</span></td>")
                .append("<td class=\"whitespace-nowrap px-6 py-4\"><span class=\"rounded-full px-3 py-1 text-sm font-semibold " + vulnStatusClass + "\">")
                .append(vulnStatusText)
                .append("</span></td>")
                .append("</tr>");
        }

        html.append("</tbody></table></div></div>");

        // Detailed Observations section (keeping the original detailed view) - Show ALL test cases
        html.append("</section><section id=\"observations\" class=\"report-section mb-8\"><h2 class=\"mb-4 flex items-center pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-magnifying-glass mr-3 text-yellow-400\"></i>Detailed Observations</h2>");

        for (VaptTestCase testCase : testCases) {
            String sevClass = "severity-" + testCase.getTestPlan().getSeverity().toString().toLowerCase();
            String statusText = getStatusDisplay(testCase.getStatus());
            String statusClass = getStatusClass(testCase.getStatus());
            html.append("<div class=\"report-card mb-6 overflow-hidden\">")
                .append("<div class=\"flex flex-wrap items-center justify-between border-b border-gray-700 bg-gray-800 p-4\">")
                .append("<h3 class=\"text-xl font-semibold text-gray-100\">")
                .append(testCase.getTestPlan().getVulnerabilityName())
                .append("</h3><div class=\"flex items-center space-x-2\"><span class=\"" + sevClass + " mt-2 rounded-full px-4 py-1 text-sm font-bold sm:mt-0\">")
                .append(testCase.getTestPlan().getSeverity())
                .append("</span><span class=\"mt-2 rounded-full px-3 py-1 text-sm font-bold " + statusClass + " sm:mt-0\">")
                .append(statusText)
                .append("</span></div></div>")
                .append("<div class=\"grid grid-cols-1 gap-4 border-b border-gray-700 p-4 md:grid-cols-2\"><div><span class=\"text-sm font-semibold text-gray-400\">Affected URL / Asset</span><p class=\"font-mono text-sm text-gray-300\">")
                .append(appName)
                .append("</p></div></div>")
                .append("<div class=\"prose prose-invert max-w-none p-4 prose-p:text-gray-300 prose-li:text-gray-300\">")
                .append("<h4 class=\"text-lg font-semibold text-gray-300\">Description</h4><p>")
                .append(testCase.getTestPlan().getDescription())
                .append("</p><h4 class=\"mt-4 text-lg font-semibold text-gray-300\">Attack Procedure</h4><p>")
                .append(testCase.getTestPlan().getTestProcedure())
                .append("</p><h4 class=\"mt-4 text-lg font-semibold text-gray-300\">Impact</h4><p>")
                .append(generateImpactDescription(testCase.getTestPlan().getSeverity()))
                .append("</p>");

            // Add PoC images and descriptions
            List<VaptPoc> pocs = getPocs(testCase.getId());
            if (!pocs.isEmpty()) {
                html.append("<h4 class=\"mt-4 text-lg font-semibold text-gray-300\">Proof of Concepts</h4>");
                for (VaptPoc poc : pocs) {
                    html.append("<div class=\"mt-2 p-3 bg-gray-800 rounded\">");
                    if (poc.getDescription() != null && !poc.getDescription().trim().isEmpty()) {
                        html.append("<p class=\"text-sm text-gray-300 mb-2\">").append(poc.getDescription()).append("</p>");
                    }
                    if (poc.getFilePath() != null && !poc.getFilePath().trim().isEmpty()) {
                        try {
                            Path filePath = Paths.get(poc.getFilePath());
                            if (Files.exists(filePath)) {
                                byte[] fileBytes = Files.readAllBytes(filePath);
                                String base64Image = Base64.getEncoder().encodeToString(fileBytes);
                                String mimeType = getMimeType(poc.getFileName());
                                html.append("<img src=\"data:").append(mimeType).append(";base64,").append(base64Image)
                                    .append("\" alt=\"").append(poc.getFileName() != null ? poc.getFileName() : "PoC Image")
                                    .append("\" class=\"max-w-full h-auto rounded border border-gray-600\" />");
                            }
                        } catch (Exception e) {
                            html.append("<p class=\"text-sm text-red-400\">Error loading image: ").append(poc.getFileName()).append("</p>");
                        }
                    }
                    html.append("</div>");
                }
            }

            html.append("</div></div>");
        }

        html.append("</section>")
            .append("<section id=\"tools\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-tools mr-3 text-blue-400\"></i>Tools Used</h2><div class=\"grid grid-cols-2 gap-4 md:grid-cols-4 lg:grid-cols-6\">")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-bug text-4xl text-green-400\"></i><span class=\"mt-2 font-semibold\">Burp Suite</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-network-wired text-4xl text-green-400\"></i><span class=\"mt-2 font-semibold\">Nmap</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-shield-cat text-4xl text-green-400\"></i><span class=\"mt-2 font-semibold\">Nessus</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-spider text-4xl text-green-400\"></i><span class=\"mt-2 font-semibold\">OWASP ZAP</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-terminal text-4xl text-green-400\"></i><span class=\"mt-2 font-semibold\">Metasploit</span></div>")
            .append("<div class=\"flex flex-col items-center rounded-lg bg-gray-900 p-4 text-center\"><i class=\"fa-solid fa-code text-4xl text-green-400\"></i><span class=\"mt-2 font-semibold\">Custom Scripts</span></div>")
            .append("</div></section>")
            .append("<section id=\"conclusion\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-flag-checkered mr-3 text-green-400\"></i>Conclusion & Recommendations</h2>")
            .append("<p class=\"mb-2 text-gray-300\">The assessment concludes with an overall posture of <strong class=\"font-mono text-lg text-gradient-cyber\">")
            .append(overallRisk)
            .append("</strong>.</p><h3 class=\"mt-4 text-lg font-semibold text-gray-300\">Key Recommendations</h3><p class=\"text-gray-400\">").append(report.getRecommendations() != null ? report.getRecommendations() : "Implement proper input validation; Regular pentests; Patch management; Security headers; Awareness training.").append("</p><h3 class=\"mt-4 text-lg font-semibold text-gray-300\">Next Steps</h3><p class=\"text-gray-400\">").append(report.getNextSteps() != null ? report.getNextSteps() : "Remediate, Retest, Monitor continuously.").append("</p>")
            .append("<div class=\"mt-12 grid grid-cols-1 gap-8 border-t border-gray-700 pt-8 md:grid-cols-3\">")
            .append("<div class=\"text-center\"><div class=\"font-mono text-lg text-gray-300\">").append(report.getPreparedBy() != null ? report.getPreparedBy() : (report.getCreatedBy() != null ? report.getCreatedBy().getUsername() : "N/A")).append("</div><div class=\"mt-2 h-0.5 w-3/4 mx-auto bg-gray-700\"></div><div class=\"mt-2 text-sm text-gray-400\">Prepared By</div></div>")
            .append("<div class=\"text-center\"><div class=\"font-mono text-lg text-gray-300\">").append(report.getReviewedBy() != null ? report.getReviewedBy() : "Security Team Lead").append("</div><div class=\"mt-2 h-0.5 w-3/4 mx-auto bg-gray-700\"></div><div class=\"mt-2 text-sm text-gray-400\">Reviewed By</div></div>")
            .append("<div class=\"text-center\"><div class=\"font-mono text-lg text-gray-300\">").append(report.getApprovedBy() != null ? report.getApprovedBy() : clientName).append("</div><div class=\"mt-2 h-0.5 w-3/4 mx-auto bg-gray-700\"></div><div class=\"mt-2 text-sm text-gray-400\">Approved By</div></div>")
            .append("</div></section></main>")
            .append("<footer class=\"print-footer hidden text-center text-xs text-gray-500\">Generated automatically by DEFECTRIX | Confidential Report | © Root Lock Defense 2025</footer>")
            .append("</body></html>");

        return html.toString();
    }


    private String getVersion() {
        return "1.0." + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private String calculateOverallRisk(Map<String, Long> severityCounts) {
        long critical = severityCounts.getOrDefault("CRITICAL", 0L);
        long high = severityCounts.getOrDefault("HIGH", 0L);
        long medium = severityCounts.getOrDefault("MEDIUM", 0L);
        long low = severityCounts.getOrDefault("LOW", 0L);

        if (critical > 0) return "Critical Risk";
        if (high > 2) return "High Risk";
        if (high > 0 || medium > 3) return "Medium Risk";
        if (medium > 0 || low > 0) return "Low Risk";
        return "Minimal Risk";
    }


    private String generateRiskDistributionChart(Map<String, Long> severityCounts) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        severityCounts.forEach((severity, count) -> {
            if (count > 0) {
                dataset.setValue(severity, count);
            }
        });

        JFreeChart chart = ChartFactory.createPieChart(
            "Risk Distribution",
            dataset,
            true,
            true,
            false
        );

        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(java.awt.Color.decode("#1a1a2e"));
        plot.setOutlinePaint(java.awt.Color.decode("#00d4ff"));

        return encodeChartToBase64(chart, 400, 300);
    }

    private String generateVulnerableAssetsChart(List<VaptTestCase> testCases) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        long passed = testCases.stream()
                .filter(tc -> tc.getStatus() == VaptTestCase.Status.PASSED)
                .count();
        long failed = testCases.stream()
                .filter(tc -> tc.getStatus() == VaptTestCase.Status.FAILED)
                .count();
        long notStarted = testCases.stream()
                .filter(tc -> tc.getStatus() == VaptTestCase.Status.NOT_STARTED)
                .count();

        dataset.addValue(passed, "Passed", "Test Cases");
        dataset.addValue(failed, "Failed", "Test Cases");
        dataset.addValue(notStarted, "Not Started", "Test Cases");

        JFreeChart chart = ChartFactory.createBarChart(
            "Test Case Status Distribution",
            "Status",
            "Count",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        chart.getPlot().setBackgroundPaint(java.awt.Color.decode("#1a1a2e"));
        chart.setBackgroundPaint(java.awt.Color.decode("#0a0a0a"));

        return encodeChartToBase64(chart, 400, 300);
    }

    private String generateTopApplicationsChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        try {
            // Attempt DB-driven aggregation if repositories support it; otherwise fallback to mock
            // Group by application name from completed test cases
            Map<String, Long> counts = vaptTestCaseRepository.findAll().stream()
                .filter(tc -> tc.getStatus() == VaptTestCase.Status.PASSED)
                .collect(Collectors.groupingBy(tc -> tc.getVaptReport().getApplication().getApplicationName(), Collectors.counting()));
            counts.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> dataset.addValue(e.getValue(), "Vulnerabilities", e.getKey()));
            if (dataset.getColumnCount() == 0) {
                dataset.addValue(5, "Vulnerabilities", "App 1");
                dataset.addValue(3, "Vulnerabilities", "App 2");
                dataset.addValue(2, "Vulnerabilities", "App 3");
                dataset.addValue(1, "Vulnerabilities", "App 4");
                dataset.addValue(1, "Vulnerabilities", "App 5");
            }
        } catch (Exception ex) {
            dataset.addValue(5, "Vulnerabilities", "App 1");
            dataset.addValue(3, "Vulnerabilities", "App 2");
            dataset.addValue(2, "Vulnerabilities", "App 3");
            dataset.addValue(1, "Vulnerabilities", "App 4");
            dataset.addValue(1, "Vulnerabilities", "App 5");
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "Top 5 Applications by Vulnerabilities",
            "Application",
            "Vulnerability Count",
            dataset,
            PlotOrientation.HORIZONTAL,
            true,
            true,
            false
        );

        chart.getPlot().setBackgroundPaint(java.awt.Color.decode("#1a1a2e"));
        chart.setBackgroundPaint(java.awt.Color.decode("#0a0a0a"));

        return encodeChartToBase64(chart, 400, 300);
    }

    private long calculateTestingDays(LocalDateTime startDate) {
        if (startDate == null) return 1;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), LocalDate.now()) + 1;
    }

    private String generateImpactDescription(TestPlan.Severity severity) {
        switch (severity) {
            case CRITICAL:
                return "Business compromise, data breach, full access with high financial & reputational loss";
            case HIGH:
                return "Unauthorized access, sensitive data exposure with major compliance violation";
            case MEDIUM:
                return "Partial compromise, data leakage, operational disruption";
            case LOW:
                return "Minor misconfiguration or info disclosure with negligible impact";
            case INFO:
                return "Informational findings with no security impact";
            default:
                return "Impact assessment pending";
        }
    }

    private String encodeChartToBase64(JFreeChart chart, int width, int height) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, width, height);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("Error encoding chart to base64: " + e.getMessage());
            return "";
        }
    }

    private String getStatusDisplay(VaptTestCase.Status status) {
        if (status == null) return "Open";
        switch (status) {
            case NOT_STARTED:
                return "Open";
            case IN_PROGRESS:
                return "In Progress";
            case PASSED:
                return "Closed";
            case FAILED:
                return "Open";  // Changed from "Failed" to "Open" for OPEN vulnerability status
            case NOT_APPLICABLE:
                return "Not Applicable";
            default:
                return status.toString();
        }
    }

    private String getStatusClass(VaptTestCase.Status status) {
        if (status == null) return "text-gray-400";
        switch (status) {
            case NOT_STARTED:
                return "text-gray-400";
            case IN_PROGRESS:
                return "text-yellow-400";
            case PASSED:
                return "text-green-400";
            case FAILED:
                return "text-red-400";
            default:
                return "text-gray-400";
        }
    }

    private String getMimeType(String fileName) {
        if (fileName == null) return "image/png";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            default: return "image/png";
        }
    }

    private int getPictureType(String mimeType) {
        switch (mimeType) {
            case "image/png": return XWPFDocument.PICTURE_TYPE_PNG;
            case "image/jpeg":
            case "image/jpg": return XWPFDocument.PICTURE_TYPE_JPEG;
            case "image/gif": return XWPFDocument.PICTURE_TYPE_GIF;
            case "image/bmp": return XWPFDocument.PICTURE_TYPE_BMP;
            default: return XWPFDocument.PICTURE_TYPE_PNG;
        }
    }

    @Transactional
    public VaptReport updateReportConfig(Long reportId, Map<String, Object> config) {
        VaptReport report = getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));

        // Freeze assessment date once it's set and report is in progress or completed
        boolean isAssessmentDateFrozen = report.getAssessmentDate() != null &&
                (report.getStatus() == VaptReport.Status.IN_PROGRESS ||
                 report.getStatus() == VaptReport.Status.COMPLETED);

        // Update fields from config map - only update non-null values
        try {
            if (config.get("assessmentDate") != null && !((String) config.get("assessmentDate")).trim().isEmpty()) {
                if (isAssessmentDateFrozen) {
                    System.out.println("Assessment date is frozen for report ID: " + reportId);
                } else {
                    report.setAssessmentDate(LocalDate.parse((String) config.get("assessmentDate")));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing assessmentDate: " + config.get("assessmentDate"));
        }

        if (config.get("reportVersion") != null) {
            report.setReportVersion(((String) config.get("reportVersion")).trim());
        }
        if (config.get("preparedBy") != null) {
            report.setPreparedBy(((String) config.get("preparedBy")).trim());
        }
        if (config.get("reviewedBy") != null) {
            report.setReviewedBy(((String) config.get("reviewedBy")).trim());
        }
        if (config.get("submittedTo") != null) {
            report.setSubmittedTo(((String) config.get("submittedTo")).trim());
        }
        if (config.get("objective") != null) {
            report.setObjective(((String) config.get("objective")).trim());
        }
        if (config.get("scope") != null) {
            report.setScope(((String) config.get("scope")).trim());
        }
        if (config.get("approach") != null) {
            report.setApproach(((String) config.get("approach")).trim());
        }
        if (config.get("keyHighlights") != null) {
            report.setKeyHighlights(((String) config.get("keyHighlights")).trim());
        }
        if (config.get("assetType") != null) {
            report.setAssetType(((String) config.get("assetType")).trim());
        }
        if (config.get("urls") != null) {
            report.setUrls(((String) config.get("urls")).trim());
        }

        try {
            if (config.get("startDate") != null && !((String) config.get("startDate")).trim().isEmpty()) {
                report.setStartDate(LocalDate.parse((String) config.get("startDate")));
            }
        } catch (Exception e) {
            System.err.println("Error parsing startDate: " + config.get("startDate"));
        }

        try {
            if (config.get("endDate") != null && !((String) config.get("endDate")).trim().isEmpty()) {
                report.setEndDate(LocalDate.parse((String) config.get("endDate")));
            }
        } catch (Exception e) {
            System.err.println("Error parsing endDate: " + config.get("endDate"));
        }

        if (config.get("testers") != null) {
            report.setTesters(((String) config.get("testers")).trim());
        }
        if (config.get("recommendations") != null) {
            report.setRecommendations(((String) config.get("recommendations")).trim());
        }
        if (config.get("nextSteps") != null) {
            report.setNextSteps(((String) config.get("nextSteps")).trim());
        }
        if (config.get("approvedBy") != null) {
            report.setApprovedBy(((String) config.get("approvedBy")).trim());
        }

        return vaptReportRepository.save(report);
    }

    @Transactional
    public void updateReportCounts(Long reportId) {
        VaptReport report = getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));

        List<VaptTestCase> testCases = getVaptTestCases(reportId);
        // Only count test cases with OPEN vulnerability status for summary
        Map<String, Long> severityCounts = testCases.stream()
                .filter(tc -> tc.getVulnerabilityStatus() == VaptTestCase.VulnerabilityStatus.OPEN)
                .collect(Collectors.groupingBy(
                        tc -> tc.getSeverity() != null ? tc.getSeverity().toString() : tc.getTestPlan().getSeverity().toString(),
                        Collectors.counting()
                ));

        report.setCriticalCount(severityCounts.getOrDefault("CRITICAL", 0L));
        report.setHighCount(severityCounts.getOrDefault("HIGH", 0L));
        report.setMediumCount(severityCounts.getOrDefault("MEDIUM", 0L));
        report.setLowCount(severityCounts.getOrDefault("LOW", 0L));
        report.setInfoCount(severityCounts.getOrDefault("INFO", 0L));

        String overallRisk = calculateOverallRisk(severityCounts);
        report.setOverallRisk(overallRisk);
        report.setPostureLevel(overallRisk);

        // Update end date and total days
        report.setEndDate(LocalDate.now());
        if (report.getStartDate() != null) {
            report.setTotalDays(java.time.temporal.ChronoUnit.DAYS.between(report.getStartDate(), LocalDate.now()) + 1);
        }

        vaptReportRepository.save(report);
        // Force flush to ensure immediate database consistency
        vaptReportRepository.flush();
    }

    public boolean isReportExpired(VaptReport report) {
        if (report.getAssessmentDate() == null) {
            return false; // Not expired if no assessment date set
        }
        LocalDate expiryDate = report.getAssessmentDate().plusMonths(6);
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isReportExpired(Long reportId) {
        VaptReport report = getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));
        return isReportExpired(report);
    }

    public List<TestPlan> getTestPlansForReport(Long reportId) {
        Optional<VaptReport> report = getVaptReportById(reportId);

        // Return all test plans (since the frontend will filter based on selections)
        return testPlanRepository.findAll();
    }
}
