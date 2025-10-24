package com.defecttracker.controller;

import com.defecttracker.entity.User;
import com.defecttracker.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("message", "Auth controller is working"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = authService.register(user);
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "user", Map.of(
                            "id", registeredUser.getId(),
                            "username", registeredUser.getUsername(),
                            "email", registeredUser.getEmail(),
                            "role", registeredUser.getRole(),
                            "firstName", registeredUser.getFirstName(),
                            "lastName", registeredUser.getLastName()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            Map<String, Object> loginResult = authService.login(username, password);
            return ResponseEntity.ok(loginResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7); // Remove "Bearer " prefix
            boolean isValid = authService.validateToken(jwtToken);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }
}