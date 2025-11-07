package com.defecttracker.entity;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CLIENT;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Column(nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Role {
        ADMIN, TESTER, CLIENT, PM_DM
    }

    // Helper methods
    public String getFullName() {
        return (firstName != null ? firstName : "") +
               (lastName != null ? " " + lastName : "");
    }

    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    public boolean isTester() {
        return Role.TESTER.equals(this.role);
    }

    public boolean isClient() {
        return Role.CLIENT.equals(this.role);
    }

    public boolean isPmDm() {
        return Role.PM_DM.equals(this.role);
    }
}
