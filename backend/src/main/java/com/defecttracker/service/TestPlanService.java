package com.defecttracker.service;

import com.defecttracker.entity.TestPlan;
import com.defecttracker.entity.User;
import com.defecttracker.repository.TestPlanRepository;
import com.defecttracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TestPlanService {

    @Autowired
    private TestPlanRepository testPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public TestPlan createTestPlan(TestPlan testPlan) {
        // Set addedBy from current user context (would need Spring Security context)
        // For now, set a default user or get from context
        if (testPlan.getAddedBy() == null) {
            // This should be set from the authenticated user
            Optional<User> defaultUser = userRepository.findByUsername("admin");
            if (defaultUser.isPresent()) {
                testPlan.setAddedBy(defaultUser.get());
            }
        }

        // Generate test case ID
        Long maxId = testPlanRepository.findMaxTestCaseIdNumber();
        String testCaseId = TestPlan.generateNextTestCaseId(maxId != null ? maxId : 0L);
        testPlan.setTestCaseId(testCaseId);

        return testPlanRepository.save(testPlan);
    }

    public List<TestPlan> getAllTestPlans() {
        return testPlanRepository.findAll();
    }

    public Page<TestPlan> getTestPlans(Pageable pageable) {
        return testPlanRepository.findAll(pageable);
    }

    public Optional<TestPlan> getTestPlanById(Long id) {
        return testPlanRepository.findById(id);
    }

    public Optional<TestPlan> getTestPlanByTestCaseId(String testCaseId) {
        return testPlanRepository.findByTestCaseId(testCaseId);
    }

    public TestPlan updateTestPlan(Long id, TestPlan testPlanDetails) {
        TestPlan testPlan = testPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test plan not found"));

        testPlan.setVulnerabilityName(testPlanDetails.getVulnerabilityName());
        testPlan.setSeverity(testPlanDetails.getSeverity());
        testPlan.setDescription(testPlanDetails.getDescription());
        testPlan.setTestProcedure(testPlanDetails.getTestProcedure());

        return testPlanRepository.save(testPlan);
    }

    public void deleteTestPlan(Long id) {
        testPlanRepository.deleteById(id);
    }

    public List<TestPlan> searchTestPlans(String search) {
        return testPlanRepository.searchTestPlans(search);
    }

    public long countTotalTestPlans() {
        return testPlanRepository.countTotalTestPlans();
    }
}
