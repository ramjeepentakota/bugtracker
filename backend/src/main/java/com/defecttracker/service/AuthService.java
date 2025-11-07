package com.defecttracker.service;

import com.defecttracker.entity.User;
import com.defecttracker.repository.UserRepository;
import com.defecttracker.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (!userOpt.isPresent()) {
            throw new RuntimeException("Invalid credentials");
        }

        User user = userOpt.get();

        boolean passwordMatches;
        if (user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$") || user.getPasswordHash().startsWith("$2y$")) {
            // BCrypt hashed password
            passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
        } else {
            // Plain text password (for development/testing)
            passwordMatches = password.equals(user.getPasswordHash());
        }

        if (!passwordMatches) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userMap);
        return response;
    }

    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Encode password with BCrypt
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setRole(User.Role.CLIENT); // Default role
        user.setActive(true);

        return userRepository.save(user);
    }

    public Optional<User> getCurrentUser() {
        // This would be implemented with Spring Security context
        return Optional.empty();
    }

    public boolean validateToken(String token) {
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }
}
