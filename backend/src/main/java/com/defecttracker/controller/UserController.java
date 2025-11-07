package com.defecttracker.controller;

import com.defecttracker.entity.User;
import com.defecttracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/testers")
    public ResponseEntity<List<User>> getTesters() {
        List<User> testers = userService.getUsersByRole(User.Role.TESTER);
        return ResponseEntity.ok(testers);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        try {
            User userDetails = new User();
            userDetails.setFirstName((String) userData.get("firstName"));
            userDetails.setLastName((String) userData.get("lastName"));
            userDetails.setEmail((String) userData.get("email"));
            userDetails.setRole(User.Role.valueOf((String) userData.get("role")));
            userDetails.setActive(Boolean.TRUE.equals(userData.get("isActive")));

            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> userData) {
        try {
            User user = new User();
            user.setFirstName(userData.get("firstName"));
            user.setLastName(userData.get("surname"));
            user.setUsername(userData.get("username")); // Generate username from firstName + lastName
            user.setEmail(userData.get("email")); // Generate email from firstName.lastName@company.com
            user.setPasswordHash(userData.get("password"));

            // Map role string to enum
            String roleStr = userData.get("role");
            switch (roleStr.toUpperCase()) {
                case "TESTER":
                    user.setRole(User.Role.TESTER);
                    break;
                case "PM":
                    user.setRole(User.Role.PM_DM); // PM maps to PM_DM
                    break;
                case "DM":
                    user.setRole(User.Role.PM_DM); // DM maps to PM_DM
                    break;
                case "ADMIN":
                    user.setRole(User.Role.ADMIN);
                    break;
                default:
                    user.setRole(User.Role.CLIENT);
            }

            User createdUser = userService.createUser(user);
            return ResponseEntity.ok(createdUser);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
