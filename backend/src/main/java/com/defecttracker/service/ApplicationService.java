package com.defecttracker.service;

import com.defecttracker.entity.Application;
import com.defecttracker.entity.User;
import com.defecttracker.repository.ApplicationRepository;
import com.defecttracker.repository.ClientRepository;
import com.defecttracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Application createApplication(Application application) {
        // Set addedBy from current user context (would need Spring Security context)
        // For now, set a default user or get from context
        if (application.getAddedBy() == null) {
            // This should be set from the authenticated user
            Optional<User> defaultUser = userRepository.findByUsername("admin");
            if (defaultUser.isPresent()) {
                application.setAddedBy(defaultUser.get());
            }
        }

        // Validate that client exists
        if (application.getClient() == null || application.getClient().getId() == null) {
            throw new RuntimeException("Client is required for application creation");
        }

        // Check if client exists
        if (!clientRepository.existsById(application.getClient().getId())) {
            throw new RuntimeException("Client not found with id: " + application.getClient().getId());
        }

        return applicationRepository.save(application);
    }

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public Page<Application> getApplications(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    public List<Application> getApplicationsByClient(Long clientId) {
        return applicationRepository.findByClientId(clientId);
    }

    public Optional<Application> getApplicationById(Long id) {
        return applicationRepository.findById(id);
    }

    public Application updateApplication(Long id, Application applicationDetails) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setApplicationName(applicationDetails.getApplicationName());
        application.setClient(applicationDetails.getClient());
        application.setEnvironment(applicationDetails.getEnvironment());
        application.setTechnologyStack(applicationDetails.getTechnologyStack());

        return applicationRepository.save(application);
    }

    public void deleteApplication(Long id) {
        applicationRepository.deleteById(id);
    }

    public List<Application> searchApplicationsByClient(Long clientId, String search) {
        return applicationRepository.searchApplicationsByClient(clientId, search);
    }

    public long countTotalApplications() {
        return applicationRepository.countTotalApplications();
    }

    public long countApplicationsByClient(Long clientId) {
        return applicationRepository.countApplicationsByClient(clientId);
    }
}
