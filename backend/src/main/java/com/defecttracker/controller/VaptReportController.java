package com.defecttracker.controller;

import com.defecttracker.entity.*;
import com.defecttracker.repository.UserRepository;
import com.defecttracker.service.VaptReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/vapt-reports")
@CrossOrigin(origins = "*")
public class VaptReportController {

    @Autowired
    private VaptReportService vaptReportService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeVaptReport(@RequestBody Map<String, Object> request) {
        Long clientId = ((Number) request.get("clientId")).longValue();
        Long applicationId = ((Number) request.get("applicationId")).longValue();
        @SuppressWarnings("unchecked")
        List<Number> rawTestPlanIds = (List<Number>) request.get("selectedTestPlanIds");
        List<Long> selectedTestPlanIds = rawTestPlanIds.stream()
                .map(Number::longValue)
                .collect(java.util.stream.Collectors.toList());

        System.out.println("Initializing VAPT report for client: " + clientId + ", application: " + applicationId);
        System.out.println("Selected test plan IDs: " + selectedTestPlanIds);

        User currentUser = getCurrentUser();
        VaptReport vaptReport = vaptReportService.getOrCreateVaptReport(clientId, applicationId, selectedTestPlanIds, currentUser);

        System.out.println("VAPT report created/loaded with ID: " + vaptReport.getId());

        // Fetch test cases immediately after report creation/loading to ensure they're available
        List<VaptTestCase> testCases = vaptReportService.getVaptTestCases(vaptReport.getId());
        System.out.println("Fetched " + testCases.size() + " test cases for report ID: " + vaptReport.getId());

        if (testCases.isEmpty()) {
            System.err.println("WARNING: No test cases were created for report ID: " + vaptReport.getId());
        }

        // Return both report and test cases in the response
        Map<String, Object> response = new HashMap<>();
        response.put("report", vaptReport);
        response.put("testCases", testCases);
        response.put("isExisting", vaptReport.getStatus() != VaptReport.Status.INITIALIZED); // Indicate if this is an existing report

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reportId}/test-cases")
    public ResponseEntity<List<VaptTestCase>> getVaptTestCases(@PathVariable Long reportId) {
        System.out.println("Fetching test cases for report ID: " + reportId);
        List<VaptTestCase> testCases = vaptReportService.getVaptTestCases(reportId);
        System.out.println("Found " + testCases.size() + " test cases for report ID: " + reportId);
        // Ensure we return the most current data from database
        return ResponseEntity.ok(testCases);
    }

    @PutMapping("/test-cases/{testCaseId}")
    public ResponseEntity<VaptTestCase> updateVaptTestCase(@PathVariable Long testCaseId, @RequestBody Map<String, Object> request) {
        // Check if report is expired before allowing updates
        VaptTestCase existingTestCase = vaptReportService.getVaptTestCaseRepository().findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("VAPT test case not found"));

        if (vaptReportService.isReportExpired(existingTestCase.getVaptReport())) {
            throw new RuntimeException("Cannot update test case: Report has expired");
        }

        String statusStr = (String) request.get("status");
        // Remove status parameter - backend will determine based on vulnerabilityStatus
        VaptTestCase.Status status = null; // Let backend determine
        String findings = (String) request.get("findings");
        String description = (String) request.get("description");
        String testProcedure = (String) request.get("testProcedure");
        TestPlan.Severity severity = request.get("severity") != null ? TestPlan.Severity.valueOf((String) request.get("severity")) : null;
        VaptTestCase.VulnerabilityStatus vulnerabilityStatus = request.get("vulnerabilityStatus") != null ? VaptTestCase.VulnerabilityStatus.valueOf((String) request.get("vulnerabilityStatus")) : VaptTestCase.VulnerabilityStatus.OPEN;

        // If statusStr indicates closed, set vulnerabilityStatus to CLOSED
        if ("closed".equalsIgnoreCase(statusStr) || "CLOSED".equals(statusStr)) {
            vulnerabilityStatus = VaptTestCase.VulnerabilityStatus.CLOSED;
        } else if ("open".equalsIgnoreCase(statusStr) || "OPEN".equals(statusStr)) {
            vulnerabilityStatus = VaptTestCase.VulnerabilityStatus.OPEN;
        }

        User currentUser = getCurrentUser();
        VaptTestCase updatedTestCase = vaptReportService.updateVaptTestCase(testCaseId, status, findings, description, testProcedure, severity, vulnerabilityStatus, currentUser);

        // Update report status to IN_PROGRESS if not already completed
        VaptReport report = updatedTestCase.getVaptReport();
        if (report.getStatus() == VaptReport.Status.INITIALIZED) {
            report.setStatus(VaptReport.Status.IN_PROGRESS);
            vaptReportService.updateReportConfig(report.getId(), new HashMap<>());
        }

        // Return updated test case with current database state
        return ResponseEntity.ok(updatedTestCase);
    }

    @PostMapping("/test-cases/{testCaseId}/pocs")
    public ResponseEntity<VaptPoc> uploadPoc(@PathVariable Long testCaseId, @RequestParam("file") MultipartFile file, @RequestParam(value = "description", required = false) String description) throws IOException {
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null ||
            (!contentType.startsWith("image/") &&
             !contentType.equals("application/pdf") &&
             !contentType.equals("application/msword") &&
             !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            return ResponseEntity.badRequest().build();
        }
        // Save file to uploads directory
        String uploadDir = "/app/uploads/vapt-pocs/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        String relativePath = uploadDir + fileName;

        User currentUser = getCurrentUser();
        VaptPoc poc = vaptReportService.addPoc(testCaseId, file.getOriginalFilename(), relativePath, description, null, currentUser);

        return ResponseEntity.ok(poc);
    }

    @PutMapping("/pocs/{pocId}")
    public ResponseEntity<VaptPoc> updatePoc(@PathVariable Long pocId, @RequestBody Map<String, String> request) {
        String description = request.get("description");

        User currentUser = getCurrentUser();
        VaptPoc updatedPoc = vaptReportService.updatePoc(pocId, description, currentUser);

        return ResponseEntity.ok(updatedPoc);
    }

    @GetMapping("/test-cases/{testCaseId}/pocs")
    public ResponseEntity<List<VaptPoc>> getPocs(@PathVariable Long testCaseId) {
        List<VaptPoc> pocs = vaptReportService.getPocs(testCaseId);
        return ResponseEntity.ok(pocs);
    }

    @DeleteMapping("/pocs/{pocId}")
    public ResponseEntity<Void> deletePoc(@PathVariable Long pocId) {
        User currentUser = getCurrentUser();
        vaptReportService.deletePoc(pocId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reportId}/generate")
    public ResponseEntity<Map<String, String>> generateReport(@PathVariable Long reportId) {
        try {
            System.out.println("Starting report generation for report ID: " + reportId);

            // Check if report exists
            VaptReport report = vaptReportService.getVaptReportById(reportId)
                    .orElseThrow(() -> new RuntimeException("VAPT report not found"));
            System.out.println("Report found: " + report.getId());

            // Validate that all selected test cases have required fields filled
            List<VaptTestCase> testCases = vaptReportService.getVaptTestCases(reportId);
            List<String> validationErrors = new java.util.ArrayList<>();

            for (VaptTestCase testCase : testCases) {
                if (testCase.getDescription() == null || testCase.getDescription().trim().isEmpty()) {
                    validationErrors.add("Test case '" + testCase.getTestPlan().getVulnerabilityName() + "' is missing description");
                }
                if (testCase.getTestProcedure() == null || testCase.getTestProcedure().trim().isEmpty()) {
                    validationErrors.add("Test case '" + testCase.getTestPlan().getVulnerabilityName() + "' is missing test procedure");
                }
            }

            if (!validationErrors.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Validation failed: " + String.join("; ", validationErrors));
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Update report status to COMPLETED
            report.setStatus(VaptReport.Status.COMPLETED);
            vaptReportService.updateReportConfig(reportId, new HashMap<>()); // Save status change

            // Generate HTML report file
            try {
                String htmlUrl = vaptReportService.generateHtmlReportFile(reportId);
                System.out.println("HTML report generated: " + htmlUrl);
            } catch (Exception e) {
                System.err.println("Error generating HTML report: " + e.getMessage());
                e.printStackTrace();
                // Continue with other formats even if HTML fails
            }

            // Generate DOCX report
            try {
                String docxUrl = vaptReportService.generateDocxReport(reportId);
                System.out.println("DOCX report generated: " + docxUrl);
            } catch (Exception e) {
                System.err.println("Error generating DOCX report: " + e.getMessage());
                e.printStackTrace();
                // Continue with PDF generation even if DOCX fails
            }

            // Generate PDF report
            try {
                System.out.println("Generating PDF report for report ID: " + reportId);
                String pdfUrl = vaptReportService.generatePdfReport(reportId);
                System.out.println("PDF report generated: " + pdfUrl);
            } catch (Exception e) {
                System.err.println("Error generating PDF report: " + e.getMessage());
                e.printStackTrace();
                // Continue even if PDF fails
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Report generation completed successfully");
            response.put("htmlUrl", "/api/vapt-reports/download/" + reportId + "/html");
            response.put("docxUrl", "/api/vapt-reports/download/" + reportId + "/docx");
            response.put("pdfUrl", "/api/vapt-reports/download/" + reportId + "/pdf");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to generate report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/download/{reportId}/html")
    public ResponseEntity<Resource> downloadHtmlReport(@PathVariable Long reportId) {
        try {
            String reportsDir = "/app/reports/";
            String fileName = "vapt-report-" + reportId + ".html";
            Path filePath = Paths.get(reportsDir, fileName);

            // Always regenerate to avoid stale reports
            vaptReportService.generateHtmlReportFile(reportId);

            Resource resource = new FileSystemResource(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{reportId}/docx")
    public ResponseEntity<Resource> downloadDocxReport(@PathVariable Long reportId) {
        try {
            String reportsDir = "/app/reports/";
            String fileName = "vapt-report-" + reportId + ".docx";
            Path filePath = Paths.get(reportsDir, fileName);

            // Always regenerate to avoid stale reports
            vaptReportService.generateDocxReport(reportId);

            Resource resource = new FileSystemResource(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{reportId}/pdf")
    public ResponseEntity<Resource> downloadPdfReport(@PathVariable Long reportId) {
        try {
            String reportsDir = "/app/reports/";
            String fileName = "vapt-report-" + reportId + ".pdf";
            Path filePath = Paths.get(reportsDir, fileName);

            // Check if file exists, if not generate it
            if (!Files.exists(filePath)) {
                System.out.println("PDF file does not exist, generating...");
                vaptReportService.generatePdfReport(reportId);
            }

            // Double check file exists after generation
            if (!Files.exists(filePath)) {
                System.err.println("PDF file still does not exist after generation attempt");
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            // Verify file size
            long fileSize = Files.size(filePath);
            System.out.println("PDF file size: " + fileSize + " bytes");

            if (fileSize == 0) {
                System.err.println("PDF file exists but is empty");
                return ResponseEntity.internalServerError().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            System.err.println("Error downloading PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private VaptTestCase.Status mapStatusString(String statusStr) {
        if (statusStr == null) return VaptTestCase.Status.NOT_STARTED;
        switch (statusStr.toLowerCase()) {
            case "open":
                return VaptTestCase.Status.NOT_STARTED;
            case "in progress":
            case "in_progress":
                return VaptTestCase.Status.IN_PROGRESS;
            case "closed":
                return VaptTestCase.Status.PASSED; // Closed means PASSED
            case "not applicable":
            case "not_applicable":
                return VaptTestCase.Status.NOT_APPLICABLE;
            default:
                try {
                    return VaptTestCase.Status.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return VaptTestCase.Status.NOT_STARTED;
                }
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> getVaptReport(@PathVariable Long reportId) {
        VaptReport report = vaptReportService.getVaptReportById(reportId)
                .orElseThrow(() -> new RuntimeException("VAPT report not found"));

        boolean isExpired = vaptReportService.isReportExpired(report);

        Map<String, Object> response = new HashMap<>();
        response.put("report", report);
        response.put("isExpired", isExpired);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{reportId}/config")
    public ResponseEntity<VaptReport> updateReportConfig(@PathVariable Long reportId, @RequestBody Map<String, Object> config) {
        try {
            // Check if report is expired before allowing config updates
            if (vaptReportService.isReportExpired(reportId)) {
                throw new RuntimeException("Cannot update report configuration: Report has expired");
            }

            System.out.println("Updating report config for report ID: " + reportId);
            System.out.println("Config data: " + config);
            VaptReport updatedReport = vaptReportService.updateReportConfig(reportId, config);
            System.out.println("Report config updated successfully");
            return ResponseEntity.ok(updatedReport);
        } catch (Exception e) {
            System.err.println("Error updating report config: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{reportId}/html")
    public ResponseEntity<Map<String, String>> getHtmlReport(@PathVariable Long reportId) {
        try {
            String html = vaptReportService.generateHtmlReportPublic(reportId);
            Map<String, String> response = new HashMap<>();
            response.put("html", html);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{reportId}/add-test-cases")
    public ResponseEntity<Map<String, Object>> addTestCasesToReport(@PathVariable Long reportId, @RequestBody Map<String, Object> request) {
        try {
            // Check if report is expired before allowing additions
            if (vaptReportService.isReportExpired(reportId)) {
                throw new RuntimeException("Cannot add test cases: Report has expired");
            }

            @SuppressWarnings("unchecked")
            List<Number> rawTestPlanIds = (List<Number>) request.get("testPlanIds");
            List<Long> newTestPlanIds = rawTestPlanIds.stream()
                    .map(Number::longValue)
                    .collect(java.util.stream.Collectors.toList());

            System.out.println("Adding test cases to report ID: " + reportId + ", test plan IDs: " + newTestPlanIds);

            VaptReport report = vaptReportService.getVaptReportById(reportId)
                    .orElseThrow(() -> new RuntimeException("VAPT report not found"));

            // Get existing test cases to avoid duplicates
            List<VaptTestCase> existingTestCases = vaptReportService.getVaptTestCases(reportId);
            List<Long> existingTestPlanIds = existingTestCases.stream()
                    .map(tc -> tc.getTestPlan().getId())
                    .collect(java.util.stream.Collectors.toList());

            // Filter out test plans that are already added
            List<Long> testPlanIdsToAdd = newTestPlanIds.stream()
                    .filter(id -> !existingTestPlanIds.contains(id))
                    .collect(java.util.stream.Collectors.toList());

            if (testPlanIdsToAdd.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "All selected test cases are already added to the report");
                response.put("addedCount", 0);
                return ResponseEntity.ok(response);
            }

            // Add new test cases
            List<TestPlan> testPlansToAdd = vaptReportService.getTestPlanRepository().findAllById(testPlanIdsToAdd);
            User currentUser = getCurrentUser();

            for (TestPlan testPlan : testPlansToAdd) {
                VaptTestCase vaptTestCase = new VaptTestCase();
                vaptTestCase.setVaptReport(report);
                vaptTestCase.setTestPlan(testPlan);
                vaptTestCase.setStatus(VaptTestCase.Status.NOT_STARTED);
                vaptTestCase.setDescription(testPlan.getDescription());
                vaptTestCase.setTestProcedure(testPlan.getTestProcedure());
                vaptTestCase.setVulnerabilityStatus(VaptTestCase.VulnerabilityStatus.OPEN);
                vaptTestCase.setUpdatedBy(null);
                vaptReportService.getVaptTestCaseRepository().save(vaptTestCase);
                System.out.println("Added new test case with ID: " + vaptTestCase.getId());
            }

            // Force flush to ensure all test cases are committed
            vaptReportService.getVaptTestCaseRepository().flush();

            // Fetch updated test cases
            List<VaptTestCase> updatedTestCases = vaptReportService.getVaptTestCases(reportId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully added " + testPlansToAdd.size() + " test cases to the report");
            response.put("addedCount", testPlansToAdd.size());
            response.put("testCases", updatedTestCases);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error adding test cases to report: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{reportId}/modify")
    public ResponseEntity<Map<String, Object>> getReportForModification(@PathVariable Long reportId) {
        try {
            System.out.println("Getting report for modification: " + reportId);

            VaptReport report = vaptReportService.getVaptReportById(reportId)
                    .orElseThrow(() -> new RuntimeException("VAPT report not found"));

            List<VaptTestCase> testCases = vaptReportService.getVaptTestCases(reportId);

            // Get all available test plans for the client/application
            List<TestPlan> availableTestPlans = vaptReportService.getTestPlansForReport(reportId);

            Map<String, Object> response = new HashMap<>();
            response.put("report", report);
            response.put("testCases", testCases);
            response.put("availableTestPlans", availableTestPlans);
            response.put("selectedTestPlanIds", testCases.stream()
                    .map(tc -> tc.getTestPlan().getId())
                    .collect(java.util.stream.Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting report for modification: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
