package com.defecttracker.repository;

import com.defecttracker.entity.DefectHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefectHistoryRepository extends JpaRepository<DefectHistory, Long> {

    @Query("SELECT dh FROM DefectHistory dh WHERE dh.defect.id = :defectId ORDER BY dh.changedAt DESC")
    List<DefectHistory> findByDefectIdOrderByChangedAtDesc(@Param("defectId") Long defectId);
}
