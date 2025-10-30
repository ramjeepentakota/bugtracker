package com.defecttracker.controller;

import com.defecttracker.entity.TestPlan;
import com.defecttracker.service.TestPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-plans")
@CrossOrigin(origins = "*")
public class TestPlanController {

    @Autowired
    private TestPlanService testPlanService;

    @PostMapping
    public ResponseEntity<TestPlan> createTestPlan(@RequestBody TestPlan testPlan) {
        TestPlan createdTestPlan = testPlanService.createTestPlan(testPlan);
        return ResponseEntity.ok(createdTestPlan);
    }

    @GetMapping
    public ResponseEntity<List<TestPlan>> getAllTestPlans() {
        List<TestPlan> testPlans = testPlanService.getAllTestPlans();
        return ResponseEntity.ok(testPlans);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<TestPlan>> getTestPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TestPlan> testPlans = testPlanService.getTestPlans(pageable);
        return ResponseEntity.ok(testPlans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestPlan> getTestPlanById(@PathVariable Long id) {
        return testPlanService.getTestPlanById(id)
                .map(testPlan -> ResponseEntity.ok(testPlan))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/test-case/{testCaseId}")
    public ResponseEntity<TestPlan> getTestPlanByTestCaseId(@PathVariable String testCaseId) {
        return testPlanService.getTestPlanByTestCaseId(testCaseId)
                .map(testPlan -> ResponseEntity.ok(testPlan))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestPlan> updateTestPlan(@PathVariable Long id, @RequestBody TestPlan testPlan) {
        try {
            TestPlan updatedTestPlan = testPlanService.updateTestPlan(id, testPlan);
            return ResponseEntity.ok(updatedTestPlan);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestPlan(@PathVariable Long id) {
        testPlanService.deleteTestPlan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TestPlan>> searchTestPlans(@RequestParam String query) {
        List<TestPlan> testPlans = testPlanService.searchTestPlans(query);
        return ResponseEntity.ok(testPlans);
    }
}
