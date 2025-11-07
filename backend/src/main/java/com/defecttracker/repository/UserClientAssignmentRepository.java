package com.defecttracker.repository;

import com.defecttracker.entity.UserClientAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserClientAssignmentRepository extends JpaRepository<UserClientAssignment, Long> {

    List<UserClientAssignment> findByUserId(Long userId);

    List<UserClientAssignment> findByClientId(Long clientId);

    List<UserClientAssignment> findByUserIdAndClientId(Long userId, Long clientId);

    @Query("SELECT COUNT(uca) > 0 FROM UserClientAssignment uca WHERE uca.user.id = :userId AND uca.client.id = :clientId AND (uca.application.id = :applicationId OR uca.application.id IS NULL)")
    boolean existsByUserIdAndClientIdOrApplicationId(@Param("userId") Long userId, @Param("clientId") Long clientId, @Param("applicationId") Long applicationId);
}