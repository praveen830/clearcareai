package com.clearcareai.modules.patient.serviceimpl;

import com.clearcareai.exception.ResourceNotFoundException;
import com.clearcareai.modules.auth.entity.User;
import com.clearcareai.modules.auth.repository.UserRepository;
import com.clearcareai.modules.patient.dto.PatientRequestDto;
import com.clearcareai.modules.patient.dto.PatientResponseDto;
import com.clearcareai.modules.patient.entity.Patient;
import com.clearcareai.modules.patient.mapper.PatientMapper;
import com.clearcareai.modules.patient.repository.PatientRepository;
import com.clearcareai.modules.patient.service.PatientService;
import com.clearcareai.modules.patient.validator.PatientValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;
    private final PatientValidator patientValidator;

    @Override
    @Transactional
    public PatientResponseDto createProfile(String email, PatientRequestDto requestDto) {
        // the email came from the JWT subject, so this is the caller
        User user = getUserByEmail(email);

        // check first for a friendly message; the unique key on user_id is
        // what actually guarantees it under concurrency (409 via B1's handler)
        boolean profileExists = patientRepository.existsByUserId(user.getId());
        patientValidator.validateProfileDoesNotExist(profileExists);

        Patient patient = patientMapper.toEntity(requestDto);

        // THE OWNERSHIP MODEL: the FK comes from the token, never from the body.
        // PatientRequestDto has no userId field for a client to lie in.
        patient.setUser(user);

        Patient saved = patientRepository.save(patient);
        log.info("Created patient profile for user: {}", email);

        return patientMapper.toResponseDto(saved);
    }

    @Override
    public PatientResponseDto getMyProfile(String email) {
        User user = getUserByEmail(email);

        // scoped to the caller's user id - the query IS the ownership check
        Optional<Patient> optionalPatient = patientRepository.findByUserId(user.getId());
        if (!optionalPatient.isPresent()) {
            throw new ResourceNotFoundException("Patient profile not found for user: " + email);
        }
        Patient patient = optionalPatient.get();

        return patientMapper.toResponseDto(patient);
    }

    @Override
    public PatientResponseDto getPatientById(Long id) {
        // no email parameter: this endpoint is ROLE_DOCTOR / ROLE_ADMIN only
        Optional<Patient> optionalPatient = patientRepository.findById(id);
        if (!optionalPatient.isPresent()) {
            throw new ResourceNotFoundException("Patient", "id", id);
        }
        Patient patient = optionalPatient.get();

        return patientMapper.toResponseDto(patient);
    }

    @Override
    @Transactional
    public PatientResponseDto updateProfile(String email, PatientRequestDto requestDto) {
        User user = getUserByEmail(email);

        Optional<Patient> optionalPatient = patientRepository.findByUserId(user.getId());
        if (!optionalPatient.isPresent()) {
            throw new ResourceNotFoundException("Patient profile not found for user: " + email);
        }
        Patient patient = optionalPatient.get();

        // note: MapStruct's default nulls out anything the DTO omits.
        // that is correct PUT semantics - full replacement.
        patientMapper.updateEntityFromDto(requestDto, patient);

        // redundant inside the transaction (the entity is managed and dirty
        // checking would flush it anyway) but it documents intent and survives
        // a later removal of @Transactional
        Patient saved = patientRepository.save(patient);
        log.info("Updated patient profile for user: {}", email);

        return patientMapper.toResponseDto(saved);
    }

    // a THROWING helper: the email came from a signed token, so a missing user
    // row means the token references a user who no longer exists - a genuine
    // 404. See D4 in Part 3 section 2.3 for when the Optional form is right.
    private User getUserByEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            return optionalUser.get();
        }
        throw new ResourceNotFoundException("User", "email", email);
    }
}