package com.clearcareai.modules.patient.serviceimpl;

import com.clearcareai.exception.ResourceNotFoundException;
import com.clearcareai.modules.auth.entity.User;
import com.clearcareai.modules.auth.repository.UserRepository;
import com.clearcareai.modules.patient.dto.PatientRequestDto;
import com.clearcareai.modules.patient.dto.PatientResponseDto;
import com.clearcareai.modules.patient.entity.Patient;
import com.clearcareai.modules.patient.exception.PatientException;
import com.clearcareai.modules.patient.mapper.PatientMapper;
import com.clearcareai.modules.patient.repository.PatientRepository;
import com.clearcareai.modules.patient.validator.PatientValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// no Spring. four mocks, one real object under test.
@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private PatientValidator patientValidator;

    @InjectMocks
    private PatientServiceImpl patientServiceImpl;

    // captures the Patient handed to save(), so we can inspect it
    @Captor
    private ArgumentCaptor<Patient> patientCaptor;

    private static final String EMAIL = "rahul@example.com";

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email(EMAIL)
                .firstName("Rahul")
                .lastName("Sharma")
                .phone("9876543210")
                .role(User.Role.ROLE_PATIENT)
                .build();
    }

    private PatientRequestDto buildRequestDto() {
        PatientRequestDto dto = new PatientRequestDto();
        dto.setDateOfBirth(LocalDate.of(1998, 5, 15));
        dto.setGender("MALE");
        dto.setBloodGroup("O+");
        return dto;
    }

    private Patient buildPatient(User user) {
        return Patient.builder()
                .id(7L)
                .user(user)
                .dateOfBirth(LocalDate.of(1998, 5, 15))
                .gender(Patient.Gender.MALE)
                .bloodGroup("O+")
                .build();
    }

    private PatientResponseDto buildResponseDto() {
        return PatientResponseDto.builder()
                .id(7L)
                .userId(1L)
                .firstName("Rahul")
                .email(EMAIL)
                .gender("MALE")
                .build();
    }

    @Test
    void createProfile_savesTheProfileAndReturnsTheDto() {
        User user = buildUser();
        Patient mapped = Patient.builder().dateOfBirth(LocalDate.of(1998, 5, 15)).build();
        Patient saved = buildPatient(user);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.existsByUserId(1L)).thenReturn(false);
        when(patientMapper.toEntity(any())).thenReturn(mapped);
        when(patientRepository.save(any())).thenReturn(saved);
        when(patientMapper.toResponseDto(saved)).thenReturn(buildResponseDto());

        PatientResponseDto result = patientServiceImpl.createProfile(EMAIL, buildRequestDto());

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        verify(patientValidator, times(1)).validateProfileDoesNotExist(false);
    }

    // THE OWNERSHIP TEST. the user on the saved entity must come from the
    // email we were given, not from anything in the request DTO
    @Test
    void createProfile_setsTheUserFromTheAuthenticatedEmail_notFromTheRequest() {
        User user = buildUser();
        Patient mapped = Patient.builder().dateOfBirth(LocalDate.of(1998, 5, 15)).build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.existsByUserId(1L)).thenReturn(false);
        when(patientMapper.toEntity(any())).thenReturn(mapped);
        when(patientRepository.save(any())).thenReturn(buildPatient(user));
        when(patientMapper.toResponseDto(any())).thenReturn(buildResponseDto());

        patientServiceImpl.createProfile(EMAIL, buildRequestDto());

        verify(patientRepository).save(patientCaptor.capture());
        Patient passedToSave = patientCaptor.getValue();
        assertThat(passedToSave.getUser()).isSameAs(user);
        assertThat(passedToSave.getUser().getId()).isEqualTo(1L);
    }

    @Test
    void createProfile_whenAProfileExists_throwsAndNeverSaves() {
        User user = buildUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.existsByUserId(1L)).thenReturn(true);
        doThrow(new PatientException("Patient profile already exists for this user"))
                .when(patientValidator).validateProfileDoesNotExist(true);

        assertThatThrownBy(() -> patientServiceImpl.createProfile(EMAIL, buildRequestDto()))
                .isInstanceOf(PatientException.class)
                .hasMessage("Patient profile already exists for this user");

        // proving the negative matters as much as proving the positive
        verify(patientRepository, never()).save(any());
    }

    @Test
    void createProfile_whenTheEmailHasNoUser_throwsResourceNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientServiceImpl.createProfile(EMAIL, buildRequestDto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with email: " + EMAIL);

        verify(patientRepository, never()).save(any());
    }

    @Test
    void getMyProfile_returnsTheCallersOwnProfile() {
        User user = buildUser();
        Patient patient = buildPatient(user);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponseDto(patient)).thenReturn(buildResponseDto());

        PatientResponseDto result = patientServiceImpl.getMyProfile(EMAIL);

        assertThat(result.getId()).isEqualTo(7L);
        // the lookup is scoped to the caller's user id - that IS the
        // ownership check, and this verify is what pins it
        verify(patientRepository).findByUserId(1L);
    }

    // a single-resource endpoint MAY 404. this is D4-compliant, not a
    // D4 violation - see Part 3 section 2.3
    @Test
    void getMyProfile_whenNoProfileExists_throwsResourceNotFound() {
        User user = buildUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientServiceImpl.getMyProfile(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Patient profile not found for user: " + EMAIL);
    }

    @Test
    void updateProfile_mapsOntoTheExistingEntityAndSaves() {
        User user = buildUser();
        Patient existing = buildPatient(user);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(existing)).thenReturn(existing);
        when(patientMapper.toResponseDto(existing)).thenReturn(buildResponseDto());

        PatientRequestDto requestDto = buildRequestDto();
        patientServiceImpl.updateProfile(EMAIL, requestDto);

        // the DTO is mapped ONTO the entity we loaded - not onto a new one
        verify(patientMapper).updateEntityFromDto(requestDto, existing);
        verify(patientRepository).save(existing);
    }

    @Test
    void updateProfile_whenNoProfileExists_throwsAndNeverSaves() {
        User user = buildUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientServiceImpl.updateProfile(EMAIL, buildRequestDto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Patient profile not found for user: " + EMAIL);

        verify(patientRepository, never()).save(any());
    }
}