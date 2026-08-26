package com.clearcareai.modules.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="refresh_tokens")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(nullable=false,unique = true,length = 500)
    private String token;
    @Column(name="expiry_date",nullable=false)
     private LocalDateTime expiryDate;
     @Column(name="is_revoked")
     @Builder.Default
     private Boolean isRevoked = false;
     @Column(name="created_at",nullable=false,updatable=false)
     @CreationTimestamp
     private LocalDateTime createdAt;

    

}

