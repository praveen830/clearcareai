package com.clearcareai.modules.patient.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.clearcareai.modules.auth.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="patient")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false,unique =true)
    private User user;
    @Column(name="date_of_birth",nullable = false)
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    @Column(name="blood_group",length = 5)
    private String bloodGroup;
    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(columnDefinition = "TEXT",name="medical_history")
    private String medicalHistory;
    @CreationTimestamp
    @Column(name="created_at",updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
    public enum Gender {
        MALE, FEMALE, OTHER
    }
}

    
