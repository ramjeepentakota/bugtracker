package com.defecttracker.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.Convert;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vapt_test_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VaptTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vapt_report_id", nullable = false)
    @JsonIgnore
    private VaptReport vaptReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_plan_id", nullable = false)
    private TestPlan testPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NOT_STARTED;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "test_procedure", columnDefinition = "TEXT")
    private String testProcedure;

    @Enumerated(EnumType.STRING)
    private TestPlan.Severity severity;

    @Column(name = "vulnerability_status", nullable = false)
    @Convert(converter = VulnerabilityStatusConverter.class)
    private VulnerabilityStatus vulnerabilityStatus = VulnerabilityStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = true)
    @JsonIgnore
    private User updatedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vaptTestCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VaptPoc> pocs;

    public enum Status {
        NOT_STARTED, IN_PROGRESS, PASSED, FAILED, NOT_APPLICABLE
    }

    public enum VulnerabilityStatus {
        OPEN, CLOSED
    }
}
