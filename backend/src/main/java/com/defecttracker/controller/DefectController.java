package com.defecttracker.controller;

import com.defecttracker.entity.Defect;
import com.defecttracker.entity.DefectHistory;
import com.defecttracker.service.DefectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/defects")
@CrossOrigin(origins = "*")
public class DefectController {

    @Autowired
    private DefectService defectService;

    @PostMapping
    public ResponseEntity<Defect> createDefect(@RequestBody Defect defect) {
        Defect createdDefect = defectService.createDefect(defect);
        return ResponseEntity.ok(createdDefect);
    }

    @PostMapping("/upload")
    public ResponseEntity<Defect> createDefectWithFile(
            @RequestParam("clientId") Long clientId,
            @RequestParam("applicationId") Long applicationId,
            @RequestParam("testPlanId") Long testPlanId,
            @RequestParam("severity") String severity,
            @RequestParam("description") String description,
            @RequestParam(value = "testingProcedure", required = false) String testingProcedure,
            @RequestParam(value = "assignedToId", required = false) Long assignedToId,
            @RequestParam(value = "status", defaultValue = "NEW") String status,
            @RequestParam(value = "pocFile", required = false) MultipartFile pocFile) {

        Defect defect = new Defect();
        defect.setClient(new com.defecttracker.entity.Client());
        defect.getClient().setId(clientId);
        defect.setApplication(new com.defecttracker.entity.Application());
        defect.getApplication().setId(applicationId);
        defect.setTestPlan(new com.defecttracker.entity.TestPlan());
        defect.getTestPlan().setId(testPlanId);
        defect.setSeverity(Defect.Severity.valueOf(severity));
        defect.setDescription(description);
        defect.setTestingProcedure(testingProcedure);
        if (assignedToId != null) {
            defect.setAssignedTo(new com.defecttracker.entity.User());
            defect.getAssignedTo().setId(assignedToId);
        }
        defect.setStatus(Defect.Status.valueOf(status));

        Defect createdDefect = defectService.createDefectWithFile(defect, pocFile);
        return ResponseEntity.ok(createdDefect);
    }

    @GetMapping
    public ResponseEntity<List<Defect>> getAllDefects() {
        List<Defect> defects = defectService.getAllDefects();
        return ResponseEntity.ok(defects);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Defect>> getDefects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Defect> defects = defectService.getDefects(pageable);
        return ResponseEntity.ok(defects);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Defect>> getDefectsByClient(@PathVariable Long clientId) {
        List<Defect> defects = defectService.getDefectsByClient(clientId);
        return ResponseEntity.ok(defects);
    }

    @GetMapping("/client/{clientId}/search")
    public ResponseEntity<Page<Defect>> searchDefectsByClient(
            @PathVariable Long clientId,
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Defect> defects = defectService.searchDefectsByClient(clientId, search, pageable);
        return ResponseEntity.ok(defects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Defect> getDefectById(@PathVariable Long id) {
        return defectService.getDefectById(id)
                .map(defect -> ResponseEntity.ok(defect))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/defect-id/{defectId}")
    public ResponseEntity<Defect> getDefectByDefectId(@PathVariable String defectId) {
        return defectService.getDefectByDefectId(defectId)
                .map(defect -> ResponseEntity.ok(defect))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Defect> updateDefect(@PathVariable Long id, @RequestBody Defect defect) {
        try {
            Defect updatedDefect = defectService.updateDefect(id, defect);
            return ResponseEntity.ok(updatedDefect);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDefect(@PathVariable Long id) {
        defectService.deleteDefect(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<DefectHistory>> getDefectHistory(@PathVariable Long id) {
        List<DefectHistory> history = defectService.getDefectHistory(id);
        return ResponseEntity.ok(history);
    }
}
