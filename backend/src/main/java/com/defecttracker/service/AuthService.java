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
        System.out.println("AuthService.login() called with username: " + username);
        System.out.println("AuthService.login() password provided: " + password);

        Optional<User> userOpt = userRepository.findByUsername(username);
        System.out.println("User lookup result: " + (userOpt.isPresent() ? "found" : "not found"));

        if (!userOpt.isPresent()) {
            System.out.println("Login failed: User not found for username: " + username);
            throw new RuntimeException("Invalid credentials");
        }

        User user = userOpt.get();
        System.out.println("Found user: " + user.getUsername() + ", active: " + user.isActive() + ", role: " + user.getRole());
        System.out.println("Stored password hash: " + user.getPasswordHash());

<<<<<<< HEAD
        boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
=======
        boolean passwordMatches = password.equals(user.getPasswordHash());
>>>>>>> e5f1c9cd (new dashboard design and login page fix)
        System.out.println("Password match result: " + passwordMatches + " for password: " + password);

        if (!passwordMatches) {
            System.out.println("Login failed: Password mismatch for user: " + username);
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isActive()) {
            System.out.println("Login failed: Account deactivated for user: " + username);
            throw new RuntimeException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        System.out.println("Login successful for user: " + username + ", generated token");

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

<<<<<<< HEAD
        // Encode password with BCrypt
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
=======
        // Store password as plain text (no encoding)
        // user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
>>>>>>> e5f1c9cd (new dashboard design and login page fix)
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
