package com.defecttracker.repository;

import com.defecttracker.entity.VaptPoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaptPocRepository extends JpaRepository<VaptPoc, Long> {

    List<VaptPoc> findByVaptTestCaseId(Long testCaseId);
}
