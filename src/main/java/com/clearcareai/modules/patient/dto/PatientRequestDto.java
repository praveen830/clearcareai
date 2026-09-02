package com.clearcareai.modules.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDto {
    @NotNull(message="Date of birth is required")
    @PastOrPresent(message="Date of birth cannot be in fruture")
    private LocalDate dateOfBirth;
   @NotBlank(message = "Gender is required")
@Pattern(
    regexp = "^(MALE|FEMALE|OTHER)$",
    message = "Gender must be one of MALE, FEMALE, OTHER"
)
private String gender;
private String bloodGroup;

    private String address;

    private String medicalHistory;
    
}
