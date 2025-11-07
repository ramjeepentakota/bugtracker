package com.defecttracker.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.defecttracker.entity.User;
import com.defecttracker.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        logger.info("AuthController.test() called - Backend is reachable");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Auth controller is working");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = authService.register(user);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", registeredUser.getId());
            userMap.put("username", registeredUser.getUsername());
            userMap.put("email", registeredUser.getEmail());
            userMap.put("role", registeredUser.getRole());
            userMap.put("firstName", registeredUser.getFirstName());
            userMap.put("lastName", registeredUser.getLastName());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("user", userMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        logger.info("AuthController.login() called with username: {}", credentials.get("username"));
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            Map<String, Object> loginResult = authService.login(username, password);
            logger.info("Login successful for user: {}", username);
            return ResponseEntity.ok(loginResult);
        } catch (Exception e) {
            logger.error("Login failed for user: {} - Error: {}", credentials.get("username"), e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7); // Remove "Bearer " prefix
            boolean isValid = authService.validateToken(jwtToken);
            Map<String, Boolean> response = new HashMap<>();
            response.put("valid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Boolean> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.ok(response);
        }
    }
}
