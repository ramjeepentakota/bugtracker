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
    public ResponseEntity<VaptReport> initializeVaptReport(@RequestBody Map<String, Long> request) {
        Long clientId = request.get("clientId");
        Long applicationId = request.get("applicationId");

        User currentUser = getCurrentUser();
        VaptReport vaptReport = vaptReportService.getOrCreateVaptReport(clientId, applicationId, currentUser);

        return ResponseEntity.ok(vaptReport);
    }

    @GetMapping("/{reportId}/test-cases")
    public ResponseEntity<List<VaptTestCase>> getVaptTestCases(@PathVariable Long reportId) {
        List<VaptTestCase> testCases = vaptReportService.getVaptTestCases(reportId);
        return ResponseEntity.ok(testCases);
    }

    @PutMapping("/test-cases/{testCaseId}")
    public ResponseEntity<VaptTestCase> updateVaptTestCase(@PathVariable Long testCaseId, @RequestBody Map<String, Object> request) {
        VaptTestCase.Status status = VaptTestCase.Status.valueOf((String) request.get("status"));
        String description = (String) request.get("description");
        String testProcedure = (String) request.get("testProcedure");
        String remediation = (String) request.get("remediation");

        User currentUser = getCurrentUser();
        VaptTestCase updatedTestCase = vaptReportService.updateVaptTestCase(testCaseId, status, description, testProcedure, remediation, currentUser);

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

            Map<String, String> response = Map.of(
                "message", "Report generation completed successfully",
                "docxUrl", "/api/vapt-reports/download/" + reportId + "/docx",
                "pdfUrl", "/api/vapt-reports/download/" + reportId + "/pdf"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = Map.of(
                "message", "Failed to generate report: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
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

            // Always regenerate to avoid stale reports
            vaptReportService.generatePdfReport(reportId);

            Resource resource = new FileSystemResource(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/{reportId}/html")
    public ResponseEntity<Map<String, String>> getHtmlReport(@PathVariable Long reportId) {
        try {
            String html = vaptReportService.generateHtmlReportPublic(reportId);
            return ResponseEntity.ok(Map.of("html", html));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}