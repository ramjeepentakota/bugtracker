
package com.defecttracker.service;

import com.defecttracker.entity.Client;
import com.defecttracker.entity.User;
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
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Client createClient(Client client) {
        // Set addedBy from current user context (would need Spring Security context)
        // For now, set a default user or get from context
        if (client.getAddedBy() == null) {
            // This should be set from the authenticated user
            Optional<User> defaultUser = userRepository.findByUsername("admin");
            if (defaultUser.isPresent()) {
                client.setAddedBy(defaultUser.get());
            }
        }

        // Check for duplicate email
        if (clientRepository.findByEmail(client.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + client.getEmail());
        }

        return clientRepository.save(client);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Page<Client> getClients(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    public Client updateClient(Long id, Client clientDetails) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        client.setClientName(clientDetails.getClientName());
        client.setEmail(clientDetails.getEmail());
        client.setContactNumber(clientDetails.getContactNumber());

        return clientRepository.save(client);
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    public List<Client> searchClients(String search) {
        return clientRepository.searchClients(search);
    }

    public long countTotalClients() {
        return clientRepository.countTotalClients();
    }

    public long countOpenDefectsByClient(Long clientId) {
        return clientRepository.countOpenDefectsByClient(clientId);
    }
}
