package com.defecttracker.service;

import com.defecttracker.entity.User;
import com.defecttracker.repository.UserRepository;
import com.defecttracker.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName()
            )
        );
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