package com.defecttracker.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vapt_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VaptReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.INITIALIZED;

    // New fields for enhanced report template
    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    @Column(name = "report_version")
    private String reportVersion;

    @Column(name = "prepared_by")
    private String preparedBy;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "submitted_to")
    private String submittedTo;

    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    @Column(name = "approach", columnDefinition = "TEXT")
    private String approach;

    @Column(name = "key_highlights", columnDefinition = "TEXT")
    private String keyHighlights;

    @Column(name = "overall_risk")
    private String overallRisk;

    @Column(name = "asset_type")
    private String assetType;

    @Column(name = "urls", columnDefinition = "TEXT")
    private String urls;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "total_days")
    private Long totalDays;

    @Column(name = "testers")
    private String testers;

    @Column(name = "critical_count")
    private Long criticalCount = 0L;

    @Column(name = "high_count")
    private Long highCount = 0L;

    @Column(name = "medium_count")
    private Long mediumCount = 0L;

    @Column(name = "low_count")
    private Long lowCount = 0L;

    @Column(name = "info_count")
    private Long infoCount = 0L;

    @Column(name = "posture_level")
    private String postureLevel;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "next_steps", columnDefinition = "TEXT")
    private String nextSteps;

    @Column(name = "approved_by")
    private String approvedBy;

    @OneToMany(mappedBy = "vaptReport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VaptTestCase> testCases;

    public enum Status {
        INITIALIZED, IN_PROGRESS, COMPLETED
    }
}
