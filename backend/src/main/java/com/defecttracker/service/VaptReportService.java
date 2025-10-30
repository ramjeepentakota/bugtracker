
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

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ApplicationRepository applicationRepository;


    @Transactional
    public VaptReport getOrCreateVaptReport(Long clientId, Long applicationId, User currentUser) {
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

        VaptReport savedReport = vaptReportRepository.save(vaptReport);

        // Create VaptTestCase entries for all test plans
        List<TestPlan> testPlans = testPlanRepository.findAll();
        for (TestPlan testPlan : testPlans) {
            VaptTestCase vaptTestCase = new VaptTestCase();
            vaptTestCase.setVaptReport(savedReport);
            vaptTestCase.setTestPlan(testPlan);
            vaptTestCase.setStatus(VaptTestCase.Status.OPEN);
            vaptTestCaseRepository.save(vaptTestCase);
        }

        return savedReport;
    }

    public List<VaptTestCase> getVaptTestCases(Long vaptReportId) {
        return vaptTestCaseRepository.findByVaptReportIdOrdered(vaptReportId);
    }

    @Transactional
    public VaptTestCase updateVaptTestCase(Long testCaseId, VaptTestCase.Status status, String description, String testProcedure, String remediation, User currentUser) {
        VaptTestCase vaptTestCase = vaptTestCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("VAPT test case not found"));

        vaptTestCase.setStatus(status);
        vaptTestCase.setDescription(description);
        vaptTestCase.setTestProcedure(testProcedure);
        vaptTestCase.setRemediation(remediation);
        vaptTestCase.setUpdatedBy(currentUser);

        return vaptTestCaseRepository.save(vaptTestCase);
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
        System.out.println("=== STARTING DEFECTRIX DOCX REPORT GENERATION ===");
        try {
            VaptReport report = getVaptReportById(reportId)
                    .orElseThrow(() -> new RuntimeException("VAPT report not found"));
            List<VaptTestCase> testCases = getVaptTestCases(reportId);

            Map<String, Long> severityCounts = testCases.stream()
                    .filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED)
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
                // Header block
                XWPFParagraph header = document.createParagraph();
                header.setAlignment(ParagraphAlignment.LEFT);
                XWPFRun r = header.createRun();
                r.setBold(true); r.setFontSize(16);
                r.setText("DEFECTRIX – Cybersecurity Intelligence Platform");
                r.addBreak();
                XWPFRun r2 = header.createRun();
                r2.setText("VAPT Assessment Report"); r2.setFontSize(12);
                r2.addBreak();
                XWPFRun r3 = header.createRun();
                r3.setText("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + " | Version: " + getVersion());
                r3.addBreak();

                // Document Attributes table
                XWPFParagraph attrsTitle = document.createParagraph();
                attrsTitle.setSpacingBefore(200);
                XWPFRun at = attrsTitle.createRun(); at.setBold(true); at.setText("Document Attributes"); at.setFontSize(13);
                XWPFTable attrs = document.createTable(3, 4);
                attrs.getRow(0).getCell(0).setText("Date of Assessment");
                attrs.getRow(0).getCell(1).setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                attrs.getRow(0).getCell(2).setText("Version");
                attrs.getRow(0).getCell(3).setText(getVersion());
                attrs.getRow(1).getCell(0).setText("Prepared by");
                attrs.getRow(1).getCell(1).setText(report.getCreatedBy().getUsername());
                attrs.getRow(1).getCell(2).setText("Reviewed by");
                attrs.getRow(1).getCell(3).setText("Security Team Lead");
                attrs.getRow(2).getCell(0).setText("Submitted to");
                attrs.getRow(2).getCell(1).setText(report.getClient().getClientName());
                attrs.getRow(2).getCell(2).setText("");
                attrs.getRow(2).getCell(3).setText("");

                // Executive Summary
                XWPFParagraph exTitle = document.createParagraph();
                exTitle.setSpacingBefore(300);
                XWPFRun exR = exTitle.createRun(); exR.setBold(true); exR.setText("Executive Summary"); exR.setFontSize(13);
                XWPFParagraph exBody = document.createParagraph();
                XWPFRun ex = exBody.createRun();
                ex.setText("Objective: Comprehensive Vulnerability Assessment and Penetration Testing (VAPT) of " + report.getApplication().getApplicationName() + "."); ex.addBreak();
                ex.setText("Scope: " + report.getApplication().getApplicationName() + " (" + report.getApplication().getEnvironment().toString() + ")"); ex.addBreak();
                ex.setText("Approach: Black Box Testing Methodology"); ex.addBreak();
                ex.setText("Overall Risk Exposure: " + calculateOverallRisk(severityCounts)); ex.addBreak();

                // VAPT Test Graphs
                XWPFParagraph graphsTitle = document.createParagraph();
                graphsTitle.setSpacingBefore(300);
                XWPFRun gtr = graphsTitle.createRun(); gtr.setBold(true); gtr.setText("VAPT Test Graphs"); gtr.setFontSize(13);
                // Insert charts
                insertBase64Png(document, riskDistributionChart);
                insertBase64Png(document, vulnerableAssetsChart);
                insertBase64Png(document, topApplicationsChart);

                // Auditing Scope
                XWPFParagraph scopeTitle = document.createParagraph(); scopeTitle.setSpacingBefore(300);
                XWPFRun sc = scopeTitle.createRun(); sc.setBold(true); sc.setText("Auditing Scope"); sc.setFontSize(13);
                XWPFTable scope = document.createTable(3, 2);
                scope.getRow(0).getCell(0).setText("Client Name"); scope.getRow(0).getCell(1).setText(report.getClient().getClientName());
                scope.getRow(1).getCell(0).setText("Application Name"); scope.getRow(1).getCell(1).setText(report.getApplication().getApplicationName());
                scope.getRow(2).getCell(0).setText("Environment"); scope.getRow(2).getCell(1).setText(report.getApplication().getEnvironment().toString());

                // Methodologies & Standards
                XWPFParagraph methTitle = document.createParagraph(); methTitle.setSpacingBefore(300);
                XWPFRun mr = methTitle.createRun(); mr.setBold(true); mr.setText("Methodologies & Standards"); mr.setFontSize(13);
                XWPFParagraph ml = document.createParagraph();
                XWPFRun mlr = ml.createRun();
                mlr.setText("• OWASP Testing Guide v4.2"); mlr.addBreak();
                mlr.setText("• PTES"); mlr.addBreak();
                mlr.setText("• OSSTMM"); mlr.addBreak();
                mlr.setText("• WSTG"); mlr.addBreak();
                mlr.setText("• NIST SP 800-115"); mlr.addBreak();

                // Timeframe
                XWPFParagraph tfTitle = document.createParagraph(); tfTitle.setSpacingBefore(300);
                XWPFRun tfr = tfTitle.createRun(); tfr.setBold(true); tfr.setText("VAPT Project Timeframe"); tfr.setFontSize(13);
                XWPFTable tf = document.createTable(2, 4);
                tf.getRow(0).getCell(0).setText("Start Date");
                tf.getRow(0).getCell(1).setText(report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A");
                tf.getRow(0).getCell(2).setText("End Date");
                tf.getRow(0).getCell(3).setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                tf.getRow(1).getCell(0).setText("Total Testing Days"); tf.getRow(1).getCell(1).setText(String.valueOf(calculateTestingDays(report.getCreatedAt())));
                tf.getRow(1).getCell(2).setText("Tester Names"); tf.getRow(1).getCell(3).setText(report.getCreatedBy().getUsername());

                // Risk Ratings and Threat Level
                XWPFParagraph rrTitle = document.createParagraph(); rrTitle.setSpacingBefore(300);
                XWPFRun rrr = rrTitle.createRun(); rrr.setBold(true); rrr.setText("Risk Ratings and Threat Level"); rrr.setFontSize(13);
                XWPFTable rr = document.createTable(5, 4);
                setRow(rr.getRow(0), new String[]{"Critical", "Business compromise, data breach, full access", "Red", "High financial & reputational loss"});
                setRow(rr.getRow(1), new String[]{"High", "Unauthorized access, sensitive data exposure", "Orange", "Major compliance violation"});
                setRow(rr.getRow(2), new String[]{"Medium", "Partial compromise, data leakage, DOS", "Yellow", "Operational disruption"});
                setRow(rr.getRow(3), new String[]{"Low", "Minor misconfiguration or info disclosure", "Green", "Negligible impact"});
                setRow(rr.getRow(4), new String[]{"Info", "Informational findings", "Blue", "No security impact"});

                // Vulnerability Summary
                XWPFParagraph vsTitle = document.createParagraph(); vsTitle.setSpacingBefore(300);
                XWPFRun vsr = vsTitle.createRun(); vsr.setBold(true); vsr.setText("Vulnerability Summary"); vsr.setFontSize(13);
                XWPFTable vs = document.createTable(6, 2);
                setRow(vs.getRow(0), new String[]{"Critical", String.valueOf(severityCounts.getOrDefault("CRITICAL", 0L))});
                setRow(vs.getRow(1), new String[]{"High", String.valueOf(severityCounts.getOrDefault("HIGH", 0L))});
                setRow(vs.getRow(2), new String[]{"Medium", String.valueOf(severityCounts.getOrDefault("MEDIUM", 0L))});
                setRow(vs.getRow(3), new String[]{"Low", String.valueOf(severityCounts.getOrDefault("LOW", 0L))});
                setRow(vs.getRow(4), new String[]{"Info", String.valueOf(severityCounts.getOrDefault("INFO", 0L))});
                setRow(vs.getRow(5), new String[]{"Total", String.valueOf(totalVulnerabilities)});
                insertBase64Png(document, riskDistributionChart);

                // Observations
                XWPFParagraph obTitle = document.createParagraph(); obTitle.setSpacingBefore(300);
                XWPFRun obr = obTitle.createRun(); obr.setBold(true); obr.setText("Observations"); obr.setFontSize(13);
                for (VaptTestCase testCase : testCases.stream().filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED).collect(Collectors.toList())) {
                    XWPFParagraph cTitle = document.createParagraph();
                    XWPFRun crun = cTitle.createRun(); crun.setBold(true); crun.setText("🐞 " + testCase.getTestPlan().getVulnerabilityName() + " (" + testCase.getTestPlan().getSeverity() + ")");
                    XWPFParagraph cBody = document.createParagraph();
                    XWPFRun cb = cBody.createRun();
                    cb.setText("Affected URL: " + report.getApplication().getApplicationName()); cb.addBreak();
                    cb.setText("Description: " + (testCase.getDescription() != null ? testCase.getDescription() : "N/A")); cb.addBreak();
                    cb.setText("Impact: " + generateImpactDescription(testCase.getTestPlan().getSeverity())); cb.addBreak();
                    cb.setText("Remediation: " + (testCase.getRemediation() != null ? testCase.getRemediation() : "N/A")); cb.addBreak();
                    List<VaptPoc> pocs = getPocs(testCase.getId());
                    if (!pocs.isEmpty()) {
                        cb.setText("Proof of Concepts:"); cb.addBreak();
                        for (VaptPoc poc : pocs) {
                            cb.setText("• " + poc.getFileName() + (poc.getDescription() != null && !poc.getDescription().trim().isEmpty() ? " (" + poc.getDescription() + ")" : ""));
                            cb.addBreak();
                        }
                    }
                }

                // Tools Used
                XWPFParagraph toolsTitle = document.createParagraph(); toolsTitle.setSpacingBefore(300);
                XWPFRun tlr = toolsTitle.createRun(); tlr.setBold(true); tlr.setText("Tools Used for the Assessment"); tlr.setFontSize(13);
                XWPFParagraph tl = document.createParagraph(); XWPFRun tlrun = tl.createRun();
                for (String tool : new String[]{"Burp Suite","Nmap","Nessus","OWASP ZAP","Amass/Subfinder","Dirsearch/Gobuster","Custom Scripts"}) {
                    tlrun.setText("• " + tool); tlrun.addBreak();
                }
                XWPFParagraph tln = document.createParagraph(); tln.createRun().setText("All tools used in combination with manual verification and exploitation techniques.");

                // Conclusion
                XWPFParagraph conTitle = document.createParagraph(); conTitle.setSpacingBefore(300);
                XWPFRun conr = conTitle.createRun(); conr.setBold(true); conr.setText("Conclusion"); conr.setFontSize(13);
                XWPFParagraph con = document.createParagraph(); XWPFRun conb = con.createRun();
                conb.setText("Overall application security posture: " + calculateOverallRisk(severityCounts)); conb.addBreak();
                conb.setText("Key Recommendations:"); conb.addBreak();
                for (String rec : new String[]{
                        "Implement proper input validation and sanitization",
                        "Regular security assessments and penetration testing",
                        "Keep all software and dependencies updated",
                        "Implement security headers and best practices",
                        "Conduct regular security awareness training"
                }) { conb.setText("• " + rec); conb.addBreak(); }

                // Signatures
                XWPFParagraph sig = document.createParagraph(); sig.setSpacingBefore(200);
                XWPFRun s = sig.createRun();
                s.setText("Prepared by: " + report.getCreatedBy().getUsername()); s.addBreak();
                s.setText("Reviewed by: Security Team Lead"); s.addBreak();
                s.setText("Approved by: Client Representative"); s.addBreak();

                // Footer text placeholder (Word footers require header/footer policy; simplified here)
                XWPFParagraph foot = document.createParagraph();
                XWPFRun fr = foot.createRun(); fr.addBreak(); fr.setText("Generated automatically by DEFECTRIX | Confidential Report | © Root Lock Defense 2025");

                try (FileOutputStream out = new FileOutputStream(filePath)) { document.write(out); }
            }

            System.out.println("=== DEFECTRIX DOCX REPORT GENERATED ===");
            return "/downloads/" + fileName;
        } catch (Exception e) {
            System.err.println("Error generating DOCX: " + e.getMessage());
            throw new IOException("Failed to generate DOCX report: " + e.getMessage(), e);
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
        System.out.println("=== STARTING ENHANCED PDF REPORT GENERATION ===");
        try {
            System.out.println("Step 1: Getting VAPT report for ID: " + reportId);
            VaptReport report = getVaptReportById(reportId)
                    .orElseThrow(() -> new RuntimeException("VAPT report not found"));
            System.out.println("Step 1: Report found - " + report.getId());

            System.out.println("Step 2: Generating HTML content...");
            String htmlContent = generateHtmlReport(reportId);
            System.out.println("Step 2: HTML content generated, length: " + htmlContent.length());

            // Create reports directory if it doesn't exist
            String reportsDir = "/app/reports/";
            Path reportsPath = Paths.get(reportsDir);
            System.out.println("Step 3: Checking reports directory: " + reportsDir);
            if (!Files.exists(reportsPath)) {
                System.out.println("Step 3: Creating reports directory");
                Files.createDirectories(reportsPath);
                System.out.println("Step 3: Reports directory created");
            } else {
                System.out.println("Step 3: Reports directory already exists");
            }

            String fileName = "vapt-report-" + reportId + ".pdf";
            String filePath = reportsDir + fileName;
            System.out.println("Step 4: PDF file path: " + filePath);

            // Generate PDF using enhanced HTML content
            System.out.println("Step 5: Starting enhanced PDF generation...");
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(fos);
                com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
                com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
                document.setMargins(50, 50, 50, 50);

                // Title
                com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("DEFECTRIX - Cybersecurity Intelligence Platform")
                        .setFontSize(18)
                        .setBold()
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
                document.add(title);

                com.itextpdf.layout.element.Paragraph subtitle = new com.itextpdf.layout.element.Paragraph("VAPT Assessment Report")
                        .setFontSize(14)
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
                document.add(subtitle);
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                // Report Header
                document.add(new com.itextpdf.layout.element.Paragraph("Client: " + report.getClient().getClientName()).setBold());
                document.add(new com.itextpdf.layout.element.Paragraph("Application: " + report.getApplication().getApplicationName()));
                document.add(new com.itextpdf.layout.element.Paragraph("Environment: " + report.getApplication().getEnvironment().toString()));
                document.add(new com.itextpdf.layout.element.Paragraph("Assessment Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));
                document.add(new com.itextpdf.layout.element.Paragraph("Version: " + getVersion()));
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                // Executive Summary
                com.itextpdf.layout.element.Paragraph execTitle = new com.itextpdf.layout.element.Paragraph("EXECUTIVE SUMMARY")
                        .setFontSize(16)
                        .setBold();
                document.add(execTitle);
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                List<VaptTestCase> testCases = getVaptTestCases(reportId);
                Map<String, Long> severityCounts = testCases.stream()
                        .filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED)
                        .collect(Collectors.groupingBy(
                                tc -> tc.getTestPlan().getSeverity().toString(),
                                Collectors.counting()
                        ));

                document.add(new com.itextpdf.layout.element.Paragraph("Objective: Comprehensive Vulnerability Assessment and Penetration Testing (VAPT) of " +
                    report.getApplication().getApplicationName() + " application."));
                document.add(new com.itextpdf.layout.element.Paragraph("Scope: " + report.getApplication().getApplicationName() + " (" +
                    report.getApplication().getEnvironment().toString() + " environment)"));
                document.add(new com.itextpdf.layout.element.Paragraph("Approach: Black Box Testing Methodology"));
                document.add(new com.itextpdf.layout.element.Paragraph("Overall Risk Exposure: " + calculateOverallRisk(severityCounts)));
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                // Vulnerability Summary Table
                com.itextpdf.layout.element.Paragraph summaryTitle = new com.itextpdf.layout.element.Paragraph("VULNERABILITY SUMMARY")
                        .setFontSize(14)
                        .setBold();
                document.add(summaryTitle);

                com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(new float[]{2, 1});
                table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(50));

                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Severity").setBold()));
                table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Count").setBold()));

                severityCounts.forEach((severity, count) -> {
                    table.addCell(severity);
                    table.addCell(count.toString());
                });

                long totalVulnerabilities = severityCounts.values().stream().mapToLong(Long::longValue).sum();
                table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("TOTAL").setBold()));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(totalVulnerabilities)).setBold()));

                document.add(table);
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                // Observations
                com.itextpdf.layout.element.Paragraph obsTitle = new com.itextpdf.layout.element.Paragraph("OBSERVATIONS")
                        .setFontSize(14)
                        .setBold();
                document.add(obsTitle);
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                for (VaptTestCase testCase : testCases.stream()
                        .filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED)
                        .collect(Collectors.toList())) {

                    document.add(new com.itextpdf.layout.element.Paragraph("🐞 " + testCase.getTestPlan().getVulnerabilityName())
                            .setBold()
                            .setFontSize(12));
                    document.add(new com.itextpdf.layout.element.Paragraph("Severity: " + testCase.getTestPlan().getSeverity()));

                    if (testCase.getDescription() != null && !testCase.getDescription().trim().isEmpty()) {
                        document.add(new com.itextpdf.layout.element.Paragraph("Description: " + testCase.getDescription()));
                    }

                    document.add(new com.itextpdf.layout.element.Paragraph("Impact: " + generateImpactDescription(testCase.getTestPlan().getSeverity())));

                    if (testCase.getRemediation() != null && !testCase.getRemediation().trim().isEmpty()) {
                        document.add(new com.itextpdf.layout.element.Paragraph("Remediation: " + testCase.getRemediation()));
                    }

                    // Proof of Concepts
                    List<VaptPoc> pocs = getPocs(testCase.getId());
                    if (!pocs.isEmpty()) {
                        document.add(new com.itextpdf.layout.element.Paragraph("Proof of Concepts:").setBold());
                        for (VaptPoc poc : pocs) {
                            String pocText = "• File: " + poc.getFileName();
                            if (poc.getDescription() != null && !poc.getDescription().trim().isEmpty()) {
                                pocText += " (" + poc.getDescription() + ")";
                            }
                            document.add(new com.itextpdf.layout.element.Paragraph(pocText));
                        }
                    }

                    document.add(new com.itextpdf.layout.element.Paragraph("\n"));
                }

                // Tools Used
                com.itextpdf.layout.element.Paragraph toolsTitle = new com.itextpdf.layout.element.Paragraph("TOOLS USED FOR THE ASSESSMENT")
                        .setFontSize(14)
                        .setBold();
                document.add(toolsTitle);
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                String[] tools = {"Burp Suite", "Nmap", "Nessus", "OWASP ZAP", "Amass/Subfinder", "Dirsearch/Gobuster", "Custom Scripts"};
                for (String tool : tools) {
                    document.add(new com.itextpdf.layout.element.Paragraph("• " + tool));
                }
                document.add(new com.itextpdf.layout.element.Paragraph("All tools used in combination with manual verification and exploitation techniques."));
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                // Conclusion
                com.itextpdf.layout.element.Paragraph concTitle = new com.itextpdf.layout.element.Paragraph("CONCLUSION")
                        .setFontSize(14)
                        .setBold();
                document.add(concTitle);
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                document.add(new com.itextpdf.layout.element.Paragraph("Overall application security posture: " + calculateOverallRisk(severityCounts)));
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));

                document.add(new com.itextpdf.layout.element.Paragraph("Key Recommendations:").setBold());
                String[] recommendations = {
                    "Implement proper input validation and sanitization",
                    "Regular security assessments and penetration testing",
                    "Keep all software and dependencies updated",
                    "Implement security headers and best practices",
                    "Conduct regular security awareness training"
                };
                for (String rec : recommendations) {
                    document.add(new com.itextpdf.layout.element.Paragraph("• " + rec));
                }

                // Signatures
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));
                document.add(new com.itextpdf.layout.element.Paragraph("Prepared by: " + report.getCreatedBy().getUsername()));
                document.add(new com.itextpdf.layout.element.Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));
                document.add(new com.itextpdf.layout.element.Paragraph("Reviewed by: Security Team Lead"));
                document.add(new com.itextpdf.layout.element.Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));
                document.add(new com.itextpdf.layout.element.Paragraph("Approved by: Client Representative"));
                document.add(new com.itextpdf.layout.element.Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))));

                document.close();
                System.out.println("Step 5: Enhanced PDF created successfully using iText");
            }

            // Verify file was created
            Path pdfPath = Paths.get(filePath);
            if (Files.exists(pdfPath)) {
                long fileSize = Files.size(pdfPath);
                System.out.println("Step 6: PDF file created successfully, size: " + fileSize + " bytes");
                if (fileSize == 0) {
                    System.err.println("Step 6: WARNING - PDF file exists but is empty (0 bytes)!");
                }
            } else {
                System.err.println("Step 6: ERROR - PDF file was not created!");
            }

            System.out.println("=== ENHANCED PDF REPORT GENERATION COMPLETED SUCCESSFULLY ===");
            return "/downloads/" + fileName;
        } catch (Exception e) {
            System.err.println("=== ERROR IN ENHANCED PDF REPORT GENERATION ===");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
            System.err.println("=== END ERROR LOG ===");
            throw new IOException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    public String generateHtmlReportPublic(Long reportId) throws IOException {
        return generateHtmlReport(reportId);
    }

    private String generateHtmlReport(Long reportId) throws IOException {
        VaptReport report = getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));

        List<VaptTestCase> testCases = getVaptTestCases(reportId);
        Map<String, Long> severityCounts = testCases.stream()
                .filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED)
                .collect(Collectors.groupingBy(tc -> tc.getTestPlan().getSeverity().toString(), Collectors.counting()));

        long critical = severityCounts.getOrDefault("CRITICAL", 0L);
        long high = severityCounts.getOrDefault("HIGH", 0L);
        long medium = severityCounts.getOrDefault("MEDIUM", 0L);
        long low = severityCounts.getOrDefault("LOW", 0L);
        long info = severityCounts.getOrDefault("INFO", 0L);

        String riskDistributionChart = generateRiskDistributionChart(severityCounts);
        String vulnerableAssetsChart = generateVulnerableAssetsChart(testCases);
        String topApplicationsChart = generateTopApplicationsChart();

        String clientName = report.getClient().getClientName();
        String appName = report.getApplication().getApplicationName();
        String environment = report.getApplication().getEnvironment().toString();
        String assessmentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        String version = getVersion();
        String overallRisk = calculateOverallRisk(severityCounts);
        long totalDays = calculateTestingDays(report.getCreatedAt());
        String testers = report.getCreatedBy().getUsername();

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
            .append("body{font-family:'Inter',sans-serif;background-color:#0d1117;color:#c9d1d9;}h1,h2,h3,h4,h5,h6{font-family:'Poppins',sans-serif;font-weight:600;}code,kbd,samp,pre,.font-mono{font-family:'Roboto Mono',monospace;} .bg-gradient-cyber{background-image:linear-gradient(to right,#00c6ff,#0072ff,#00d4ff,#00ff8f);} .text-gradient-cyber{background-image:linear-gradient(to right,#00c6ff,#00ff8f);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;text-fill-color:transparent;} .border-cyber-gradient{border-image-slice:1;border-image-source:linear-gradient(to right,#0072ff,#00ff8f);} .report-card{background-color:#161b22;border:1px solid #30363d;border-radius:8px;page-break-inside:avoid;} .severity-critical{background-color:#da3633;color:#ffffff;} .severity-high{background-color:#f08833;color:#ffffff;} .severity-medium{background-color:#dbab09;color:#ffffff;} .severity-low{background-color:#2f81f7;color:#ffffff;} .severity-info{background-color:#6e7681;color:#ffffff;} .text-critical{color:#f85149;} .text-high{color:#f08833;} .text-medium{color:#dbab09;} .text-low{color:#58a6ff;} .text-info{color:#8b949e;} .main-content{background-color:#0d1117;} .tab-content{display:block;} @media print{.sidebar,.print-button{display:none!important;} body{background-color:#ffffff!important;color:#000000!important;font-family:'Inter',sans-serif;} .main-content{margin-left:0!important;padding:0!important;width:100%!important;border:none!important;max-width:100%!important;} .report-card,.report-section{border:1px solid #ddd!important;box-shadow:none!important;background-color:#f9f9f9!important;color:#000!important;page-break-inside:avoid!important;} h1,h2,h3,h4,h5,h6,span,p,div{color:#000000!important;} .text-gradient-cyber{background-image:none!important;-webkit-text-fill-color:#000!important;text-fill-color:#000!important;} canvas{print-color-adjust:exact;-webkit-print-color-adjust:exact;} @page{size:A4;margin:1.5cm;} a{text-decoration:none;color:#000!important;} .print-footer{display:block!important;position:fixed;bottom:0;width:100%;text-align:center;font-size:9pt;color:#888!important;} body::after{content:'CONFIDENTIAL';color:rgba(0,0,0,0.08)!important;font-size:10vw!important;} .severity-critical,.severity-high,.severity-medium,.severity-low,.severity-info{background-color:#eee!important;color:#000!important;border:1px solid #ccc;print-color-adjust:exact;-webkit-print-color-adjust:exact;} .severity-critical{background-color:#da3633!important;color:#fff!important;} .severity-high{background-color:#f08833!important;color:#fff!important;} .severity-medium{background-color:#dbab09!important;color:#fff!important;} .severity-low{background-color:#2f81f7!important;color:#fff!important;} .severity-info{background-color:#6e7681!important;color:#fff!important;}}")
            .append("</style>")
            .append("</head>")
            .append("<body class=\"antialiased\">")
            .append("<main class=\"main-content max-w-6xl mx-auto p-8\">")
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
            .append("<div class=\"grid grid-cols-1 gap-6 lg:grid-cols-3\">")
            .append("<div class=\"space-y-4 lg:col-span-2\">")
            .append("<h3 class=\"text-lg font-semibold text-gray-300\">Objective</h3><p class=\"text-gray-400\">Comprehensive VAPT assessment</p>")
            .append("<h3 class=\"text-lg font-semibold text-gray-300\">Scope & Approach</h3><p class=\"text-gray-400\">")
            .append("Scope: ").append(appName).append(" ( ").append(environment.toString()).append(" ) ")
            .append("Approach: Black Box Testing</p>")
            .append("<h3 class=\"text-lg font-semibold text-gray-300\">Key Highlights</h3><p class=\"text-gray-400\">Automated report with charts and summaries</p>")
            .append("</div>")
            .append("<div class=\"lg:col-span-1\"><h3 class=\"mb-2 text-lg font-semibold text-gray-300\">Overall Risk Posture</h3>")
            .append("<div class=\"rounded-lg bg-gray-900 p-4\"><div class=\"mb-2 text-center text-3xl font-bold text-gradient-cyber\">")
            .append(overallRisk).append("</div><p class=\"mb-4 text-center text-sm text-gray-400\">Based on findings and impact</p>")
            .append("<img alt=\"riskPosture\" src=\"").append(riskDistributionChart).append("\" style=\"width:100%;max-height:240px;object-fit:contain\"/>")
            .append("</div></div></div></section>")
            .append("<section id=\"dashboard\" class=\"report-section report-card mb-8 p-6\">")
            .append("<h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-chart-pie mr-3 text-green-400\"></i>Management Dashboard</h2>")
            .append("<div class=\"grid grid-cols-1 gap-6 lg:grid-cols-2\">")
            .append("<div class=\"report-card border-gray-700 bg-gray-900 p-4\"><h3 class=\"mb-2 text-center font-semibold\">Risk Distribution</h3><img src=\"")
            .append(riskDistributionChart).append("\" alt=\"Risk Distribution\" /></div>")
            .append("<div class=\"report-card border-gray-700 bg-gray-900 p-4\"><h3 class=\"mb-2 text-center font-semibold\">Vulnerability Summary</h3><img src=\"")
            .append(riskDistributionChart).append("\" alt=\"Vulnerability Summary\" /></div>")
            .append("<div class=\"report-card border-gray-700 bg-gray-900 p-4\"><h3 class=\"mb-2 text-center font-semibold\">Vulnerable vs. Non-Vulnerable Assets</h3><img src=\"")
            .append(vulnerableAssetsChart).append("\" alt=\"Assets\" /></div>")
            .append("<div class=\"report-card border-gray-700 bg-gray-900 p-4\"><h3 class=\"mb-2 text-center font-semibold\">Top 5 Vulnerable Applications</h3><img src=\"")
            .append(topApplicationsChart).append("\" alt=\"Top Apps\" /></div>")
            .append("</div></section>")
            .append("<div class=\"grid grid-cols-1 gap-8 lg:grid-cols-2\">")
            .append("<section id=\"scope\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-crosshairs mr-3 text-blue-400\"></i>Auditing Scope</h2>")
            .append("<div class=\"space-y-3\">")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Client Name:</span> <span class=\"font-mono\">").append(clientName).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Application Name:</span> <span class=\"font-mono\">").append(appName).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Asset Type:</span> <span class=\"font-mono\">Web Application</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Environment:</span> <span class=\"font-mono\">").append(environment).append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">URLs / IPs:</span> <pre class=\"font-mono text-sm text-gray-300\">").append(appName).append("</pre></div>")
            .append("</div></section>")
            .append("<section id=\"timeframe\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-calendar-alt mr-3 text-blue-400\"></i>Project Timeframe</h2>")
            .append("<div class=\"space-y-3\">")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">Start Date:</span> <span class=\"font-mono\">")
            .append(report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A").append("</span></div>")
            .append("<div class=\"info-item\"><span class=\"font-semibold text-gray-400\">End Date:</span> <span class=\"font-mono\">").append(assessmentDate).append("</span></div>")
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
            .append("<div class=\"overflow-x-auto\"><table class=\"min-w-full divide-y divide-gray-700\"><thead class=\"bg-gray-800\"><tr><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Severity</th><th class=\"px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-300\">Description</th></tr></thead><tbody class=\"divide-y divide-gray-700\">")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-critical rounded-full px-3 py-1 text-sm font-semibold\">Critical</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Immediate, direct, and demonstrable impact to business, data, or systems. (e.g., RCE, Full Auth Bypass)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-high rounded-full px-3 py-1 text-sm font-semibold\">High</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Significant impact on confidentiality, integrity, or availability. (e.g., SQLi, Stored XSS, IDOR)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-medium rounded-full px-3 py-1 text-sm font-semibold\">Medium</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Potential to impact business, but requires more complex exploitation. (e.g., Reflected XSS, CSRF)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-low rounded-full px-3 py-1 text-sm font-semibold\">Low</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Minor security weakness with low or limited impact. (e.g., Info Disclosure, Weak SSL Ciphers)</td></tr>")
            .append("<tr><td class=\"whitespace-nowrap px-6 py-4\"><span class=\"severity-info rounded-full px-3 py-1 text-sm font-semibold\">Info</span></td><td class=\"whitespace-nowrap px-6 py-4 text-sm text-gray-300\">Observational finding or security best practice recommendation.</td></tr>")
            .append("</tbody></table></div></section>")
            .append("<section id=\"vuln-summary\" class=\"report-section report-card mb-8 p-6\"><h2 class=\"mb-4 flex items-center border-b border-gray-700 pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-bug-slash mr-3 text-orange-400\"></i>Vulnerability Summary</h2>")
            .append("<div class=\"grid grid-cols-1 gap-6 lg:grid-cols-2\"><div><h3 class=\"mb-2 text-lg font-semibold text-gray-300\">Findings by Severity</h3><div class=\"overflow-hidden rounded-lg border border-gray-700\"><table class=\"min-w-full divide-y divide-gray-700\"><tbody class=\"divide-y divide-gray-700\">")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-critical\">Critical</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-critical\">").append(critical).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-high\">High</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-high\">").append(high).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-medium\">Medium</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-medium\">").append(medium).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-low\">Low</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-low\">").append(low).append("</td></tr>")
            .append("<tr class=\"bg-gray-800\"><td class=\"px-4 py-3 font-semibold text-info\">Informational</td><td class=\"px-4 py-3 text-right font-mono text-lg font-bold text-info\">").append(info).append("</td></tr>")
            .append("</tbody></table></div></div>")
            .append("<div><h3 class=\"mb-2 text-lg font-semibold text-gray-300\">Executive Scorecard (Likelihood vs. Impact)</h3>")
            .append("<table class=\"w-full border-collapse border border-gray-700 text-center text-sm\"><thead class=\"bg-gray-800\"><tr><th class=\"border border-gray-700 p-2\" rowspan=\"2\" colspan=\"2\"></th><th class=\"border border-gray-700 p-2\" colspan=\"3\">Impact</th></tr><tr><th class=\"border border-gray-700 p-2\">Low</th><th class=\"border border-gray-700 p-2\">Medium</th><th class=\"border border-gray-700 p-2\">High</th></tr></thead><tbody><tr><th class=\"border border-gray-700 p-2\" rowspan=\"3\" valign=\"middle\">Likelihood</th><th class=\"border border-gray-700 p-2\">High</th><td class=\"severity-medium p-3\">Medium</td><td class=\"severity-high p-3\">High</td><td class=\"severity-critical p-3\">Critical</td></tr><tr><th class=\"border border-gray-700 p-2\">Medium</th><td class=\"severity-low p-3\">Low</td><td class=\"severity-medium p-3\">Medium</td><td class=\"severity-high p-3\">High</td></tr><tr><th class=\"border border-gray-700 p-2\">Low</th><td class=\"severity-info p-3\">Info</td><td class=\"severity-low p-3\">Low</td><td class=\"severity-medium p-3\">Medium</td></tr></tbody></table></div></div></section>")
            .append("<section id=\"observations\" class=\"report-section mb-8\"><h2 class=\"mb-4 flex items-center pb-2 text-2xl font-semibold\"><i class=\"fa-solid fa-magnifying-glass mr-3 text-yellow-400\"></i>Detailed Observations</h2>");

        for (VaptTestCase testCase : testCases.stream().filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED).collect(Collectors.toList())) {
            String sevClass = "severity-" + testCase.getTestPlan().getSeverity().toString().toLowerCase();
            html.append("<div class=\"report-card mb-6 overflow-hidden\">")
                .append("<div class=\"flex flex-wrap items-center justify-between border-b border-gray-700 bg-gray-800 p-4\">")
                .append("<h3 class=\"text-xl font-semibold text-gray-100\">")
                .append(testCase.getTestPlan().getVulnerabilityName())
                .append("</h3><span class=\"" + sevClass + " mt-2 rounded-full px-4 py-1 text-sm font-bold sm:mt-0\">")
                .append(testCase.getTestPlan().getSeverity())
                .append("</span></div>")
                .append("<div class=\"grid grid-cols-1 gap-4 border-b border-gray-700 p-4 md:grid-cols-2\"><div><span class=\"text-sm font-semibold text-gray-400\">Affected URL / Asset</span><p class=\"font-mono text-sm text-gray-300\">")
                .append(appName)
                .append("</p></div></div>")
                .append("<div class=\"prose prose-invert max-w-none p-4 prose-p:text-gray-300 prose-li:text-gray-300\">")
                .append("<h4 class=\"text-lg font-semibold text-gray-300\">Description</h4><p>")
                .append(testCase.getDescription() != null ? testCase.getDescription() : "N/A")
                .append("</p><h4 class=\"mt-4 text-lg font-semibold text-gray-300\">Impact</h4><p>")
                .append(generateImpactDescription(testCase.getTestPlan().getSeverity()))
                .append("</p><h4 class=\"mt-4 text-lg font-semibold text-gray-300\">Remediation</h4><p>")
                .append(testCase.getRemediation() != null ? testCase.getRemediation() : "N/A")
                .append("</p></div></div>");
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
            .append("</strong>.</p><h3 class=\"mt-4 text-lg font-semibold text-gray-300\">Key Recommendations</h3><p class=\"text-gray-400\">Implement proper input validation; Regular pentests; Patch management; Security headers; Awareness training.</p><h3 class=\"mt-4 text-lg font-semibold text-gray-300\">Next Steps</h3><p class=\"text-gray-400\">Remediate, Retest, Monitor continuously.</p>")
            .append("<div class=\"mt-12 grid grid-cols-1 gap-8 border-t border-gray-700 pt-8 md:grid-cols-3\">")
            .append("<div class=\"text-center\"><div class=\"font-mono text-lg text-gray-300\">").append(report.getCreatedBy().getUsername()).append("</div><div class=\"mt-2 h-0.5 w-3/4 mx-auto bg-gray-700\"></div><div class=\"mt-2 text-sm text-gray-400\">Prepared By</div></div>")
            .append("<div class=\"text-center\"><div class=\"font-mono text-lg text-gray-300\">Security Team Lead</div><div class=\"mt-2 h-0.5 w-3/4 mx-auto bg-gray-700\"></div><div class=\"mt-2 text-sm text-gray-400\">Reviewed By</div></div>")
            .append("<div class=\"text-center\"><div class=\"font-mono text-lg text-gray-300\">").append(clientName).append("</div><div class=\"mt-2 h-0.5 w-3/4 mx-auto bg-gray-700\"></div><div class=\"mt-2 text-sm text-gray-400\">Approved By</div></div>")
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

        long vulnerable = testCases.stream()
                .filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED)
                .count();
        long nonVulnerable = testCases.size() - vulnerable;

        dataset.addValue(vulnerable, "Vulnerable", "Assets");
        dataset.addValue(nonVulnerable, "Non-Vulnerable", "Assets");

        JFreeChart chart = ChartFactory.createBarChart(
            "Vulnerable vs Non-Vulnerable Assets",
            "Asset Type",
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
                .filter(tc -> tc.getStatus() == VaptTestCase.Status.COMPLETED)
                .collect(Collectors.groupingBy(tc -> tc.getVaptReport().getApplication().getApplicationName(), Collectors.counting()));
            counts.entrySet().stream()
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
}
