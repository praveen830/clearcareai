package com.clearcareai.modules.auth.dto;

import com.clearcareai.modules.auth.validator.ValidPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message="Email is required")
    @Email(message="must be valid")
private String email;
@NotBlank(message="password is required")
@ValidPassword
private String password;
@NotBlank(message="First name is required")
private String firstName;
@NotBlank(message="Last name is Required")
private String lastName;
@NotBlank(message="phone number is required")
@Pattern(regexp="\\d{10}", message="phone numbers must be exactly 10 digits")
private String phone;
@NotBlank(message="role is required")
@Pattern(regexp = "^(ROLE_PATIENT|ROLE_ADMIN|ROLE_DOCTOR)",message="role must be one of ROLE_PATIENT,ROLE_ADMIN,ROLE_DOCTOR")
private String role;
    
}
