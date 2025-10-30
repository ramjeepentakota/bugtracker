package com.defecttracker.repository;

import com.defecttracker.entity.DefectHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefectHistoryRepository extends JpaRepository<DefectHistory, Long> {

    List<DefectHistory> findByDefectIdOrderByChangedAtDesc(Long defectId);

    List<DefectHistory> findByChangedByIdOrderByChangedAtDesc(Long changedById);
}
