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
@Table(name = "defects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Defect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Defect ID is required")
    private String defectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnore
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_plan_id", nullable = false)
    @JsonIgnore
    private TestPlan testPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Description is required")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String testingProcedure;

    private String pocPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    @JsonIgnore
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "defect", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<DefectHistory> history;

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }

    public enum Status {
        NEW, OPEN, IN_PROGRESS, RETEST, CLOSED
    }

    // Helper method to generate next defect ID
    public static String generateNextDefectId(long currentMaxId) {
        return String.format("DEF-%03d", currentMaxId + 1);
    }

    // Helper methods for status checks
    public boolean isOpen() {
        return Status.OPEN.equals(this.status) || Status.IN_PROGRESS.equals(this.status) || Status.RETEST.equals(this.status);
    }

    public boolean isClosed() {
        return Status.CLOSED.equals(this.status);
    }
}
