package com.defecttracker.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Vulnerability name is required")
    private String vulnerabilityName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Test procedure is required")
    private String testProcedure;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Test case ID is required")
    private String testCaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    @JsonIgnore
    private User addedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "testPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Defect> defects;

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }

    // Helper method to generate next test case ID
    public static String generateNextTestCaseId(long currentMaxId) {
        return String.format("TP-%03d", currentMaxId + 1);
    }
}
