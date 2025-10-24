package com.defecttracker.repository;

import com.defecttracker.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    List<Client> findByClientNameContainingIgnoreCase(String clientName);

    List<Client> findByCompanyNameContainingIgnoreCase(String companyName);

    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.clientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Client> searchClients(@Param("search") String search);

    @Query("SELECT COUNT(c) FROM Client c")
    long countTotalClients();

    @Query("SELECT COUNT(d) FROM Defect d WHERE d.client.id = :clientId AND d.status IN ('NEW', 'OPEN', 'IN_PROGRESS', 'RETEST')")
    long countOpenDefectsByClient(@Param("clientId") Long clientId);
}