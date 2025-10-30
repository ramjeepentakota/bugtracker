package com.defecttracker.service;

import com.defecttracker.entity.*;
import com.defecttracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DefectService {

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private DefectHistoryRepository defectHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Defect createDefect(Defect defect) {
        // Set createdBy from current user context (would need Spring Security context)
        // For now, set a default user or get from context
        if (defect.getCreatedBy() == null) {
            // This should be set from the authenticated user
            Optional<User> defaultUser = userRepository.findByUsername("admin");
            if (defaultUser.isPresent()) {
                defect.setCreatedBy(defaultUser.get());
            }
        }

        // Generate defect ID
        Long maxId = defectRepository.findMaxDefectIdNumber();
        String defectId = Defect.generateNextDefectId(maxId != null ? maxId : 0L);
        defect.setDefectId(defectId);

        Defect savedDefect = defectRepository.save(defect);

        // Create initial history entry
        createDefectHistory(savedDefect, null, savedDefect.getStatus(), "Defect created");

        return savedDefect;
    }

    public List<Defect> getAllDefects() {
        return defectRepository.findAll();
    }

    public Page<Defect> getDefects(Pageable pageable) {
        return defectRepository.findAll(pageable);
    }

    public List<Defect> getDefectsByClient(Long clientId) {
        return defectRepository.findByClientId(clientId);
    }

    public Page<Defect> searchDefectsByClient(Long clientId, String search, Pageable pageable) {
        return defectRepository.searchDefectsByClient(clientId, search, pageable);
    }

    public Optional<Defect> getDefectById(Long id) {
        return defectRepository.findById(id);
    }

    public Optional<Defect> getDefectByDefectId(String defectId) {
        return defectRepository.findByDefectId(defectId);
    }

    @Transactional
    public Defect updateDefect(Long id, Defect defectDetails) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Defect not found"));

        Defect.Status oldStatus = defect.getStatus();

        // Update fields
        defect.setClient(defectDetails.getClient());
        defect.setApplication(defectDetails.getApplication());
        defect.setTestPlan(defectDetails.getTestPlan());
        defect.setSeverity(defectDetails.getSeverity());
        defect.setDescription(defectDetails.getDescription());
        defect.setTestingProcedure(defectDetails.getTestingProcedure());
        defect.setAssignedTo(defectDetails.getAssignedTo());
        defect.setStatus(defectDetails.getStatus());

        Defect updatedDefect = defectRepository.save(defect);

        // Create history entry if status changed
        if (!oldStatus.equals(updatedDefect.getStatus())) {
            createDefectHistory(updatedDefect, oldStatus, updatedDefect.getStatus(), "Status updated");
        }

        return updatedDefect;
    }

    public void deleteDefect(Long id) {
        defectRepository.deleteById(id);
    }

    public List<DefectHistory> getDefectHistory(Long defectId) {
        return defectHistoryRepository.findByDefectIdOrderByChangedAtDesc(defectId);
    }

    private void createDefectHistory(Defect defect, Defect.Status oldStatus, Defect.Status newStatus, String reason) {
        DefectHistory history = new DefectHistory();
        history.setDefect(defect);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangeReason(reason);
        // Set changedBy from current user context (would need Spring Security context)
        // For now, set createdBy as the changer
        history.setChangedBy(defect.getCreatedBy());

        defectHistoryRepository.save(history);
    }

    public long countTotalDefects() {
        return defectRepository.countTotalDefects();
    }

    public long countOpenDefects() {
        return defectRepository.countOpenDefects();
    }

    public long countClosedDefects() {
        return defectRepository.countClosedDefects();
    }

    @Transactional
    public Defect createDefectWithFile(Defect defect, MultipartFile file) {
        // Set createdBy from current user context (would need Spring Security context)
        // For now, set a default user or get from context
        if (defect.getCreatedBy() == null) {
            // This should be set from the authenticated user
            Optional<User> defaultUser = userRepository.findByUsername("admin");
            if (defaultUser.isPresent()) {
                defect.setCreatedBy(defaultUser.get());
            }
        }

        if (file != null && !file.isEmpty()) {
            try {
                // Create uploads directory if it doesn't exist
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
                String filename = UUID.randomUUID().toString() + extension;

                // Save file
                Path filePath = uploadPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Set POC path in defect
                defect.setPocPath(filename);

            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file: " + e.getMessage());
            }
        }

        return createDefect(defect);
    }
}
