package com.clearcareai.modules.patient.service;

import com.clearcareai.modules.patient.dto.PatientRequestDto;
import com.clearcareai.modules.patient.dto.PatientResponseDto;

public interface PatientService {
    PatientResponseDto createProfile(String email, PatientRequestDto requestDto);

    PatientResponseDto getMyProfile(String email);
    PatientResponseDto getPatientById(Long id);

    PatientResponseDto updateProfile(String email, PatientRequestDto requestDto);

    
}
