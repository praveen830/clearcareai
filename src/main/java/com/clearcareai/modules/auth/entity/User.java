package com.clearcareai.modules.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="users")
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true is the real guarantee; the code check in AuthServiceImpl
    // is only there for a friendly error message
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // no length: a BCrypt hash is 60 chars and varchar(255) is the default
    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 15)
    private String phone;

    // STRING, not ORDINAL: store the name so reordering the enum is harmless
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // @Builder.Default, or the builder ignores "= true" and writes null
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // updatable = false keeps this column out of every generated UPDATE
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // must match AppConstants.ROLE_* exactly
    public enum Role {
        ROLE_PATIENT, ROLE_DOCTOR, ROLE_ADMIN
    }
}
