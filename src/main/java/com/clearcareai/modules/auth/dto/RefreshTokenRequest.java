package com.clearcareai.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message="Refresh Token is Required")
    private String refreshToken;

    
}
