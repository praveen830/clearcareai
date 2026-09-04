package com.clearcareai.modules.patient.controller;

import com.clearcareai.common.ApiResponse;
import com.clearcareai.modules.patient.dto.PatientRequestDto;
import com.clearcareai.modules.patient.dto.PatientResponseDto;
import com.clearcareai.modules.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    // the INTERFACE, not the impl - which is what lets the test replace the
    // whole service with a @MockBean
    private final PatientService patientService;

    // 201 because this creates a resource addressable at /api/patients/{id}.
    // register returns 200 because tokens have no URL to point at.
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<PatientResponseDto>> createProfile(
            @Valid @RequestBody PatientRequestDto requestDto,
            Authentication authentication) {

        // authentication.getName() is the JWT subject - the one value in this
        // request a client cannot forge without the signing key
        PatientResponseDto response = patientService.createProfile(authentication.getName(), requestDto);

        // fromCurrentContextPath, not fromCurrentRequest: the current request
        // is /api/patients/profile, and appending to it gives a URL that isn't real
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/patients/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success("Patient profile created", response));
    }

    @GetMapping("/profile")
    public ApiResponse<PatientResponseDto> getMyProfile(Authentication authentication) {
        PatientResponseDto response = patientService.getMyProfile(authentication.getName());
        return ApiResponse.success("Patient profile retrieved", response);
    }

    // ROLE_DOCTOR / ROLE_ADMIN only - enforced by SecurityConfig, not here
    @GetMapping("/{id}")
    public ApiResponse<PatientResponseDto> getPatientById(@PathVariable Long id) {
        PatientResponseDto response = patientService.getPatientById(id);
        return ApiResponse.success("Patient retrieved", response);
    }

    @PutMapping("/profile")
    public ApiResponse<PatientResponseDto> updateProfile(
            @Valid @RequestBody PatientRequestDto requestDto,
            Authentication authentication) {
        PatientResponseDto response = patientService.updateProfile(authentication.getName(), requestDto);
        return ApiResponse.success("Patient profile updated", response);
    }
}