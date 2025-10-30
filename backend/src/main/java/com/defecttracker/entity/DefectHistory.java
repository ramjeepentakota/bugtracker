package com.defecttracker.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "defect_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefectHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defect_id", nullable = false)
    @JsonIgnore
    private Defect defect;

    @Enumerated(EnumType.STRING)
    private Defect.Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Defect.Status newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    @JsonIgnore
    private User changedBy;

    @Column(columnDefinition = "TEXT")
    private String changeReason;

    @CreationTimestamp
    private LocalDateTime changedAt;
}
