package com.defecttracker.repository;

import com.defecttracker.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByClientId(Long clientId);

    @Query("SELECT a FROM Application a WHERE a.client.id = :clientId AND " +
           "(LOWER(a.applicationName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.environment) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Application> searchApplicationsByClient(@Param("clientId") Long clientId, @Param("search") String search);

    @Query("SELECT COUNT(a) FROM Application a")
    long countTotalApplications();

    @Query("SELECT COUNT(a) FROM Application a WHERE a.client.id = :clientId")
    long countApplicationsByClient(@Param("clientId") Long clientId);
}
