package com.clearcareai.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message="email must be required")
    @Email(message="email must be valid")
    private String email;
    @NotBlank(message="password is required")
    private String password;


    
}
