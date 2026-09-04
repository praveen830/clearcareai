package com.clearcareai.modules.patient.controller;

import com.clearcareai.modules.patient.dto.PatientResponseDto;
import com.clearcareai.modules.patient.service.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// the whole context, so the REAL SecurityConfig runs - which is the point:
// this class tests the security chain as much as the controller
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // no database, no mapper, no validator: the controller depends on the
    // INTERFACE, so the whole service can be replaced with a fake
    @MockBean
    private PatientService patientService;

    private static final String EMAIL = "rahul@example.com";

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("dateOfBirth", "1998-05-15");
        body.put("gender", "MALE");
        body.put("bloodGroup", "O+");
        body.put("address", "123 Main Street, Hyderabad");
        body.put("medicalHistory", "No known allergies");
        return body;
    }

    private PatientResponseDto buildResponseDto() {
        return PatientResponseDto.builder()
                .id(1L)
                .userId(1L)
                .firstName("Rahul")
                .lastName("Sharma")
                .email(EMAIL)
                .phone("9876543210")
                .dateOfBirth(LocalDate.of(1998, 5, 15))
                .gender("MALE")
                .bloodGroup("O+")
                .build();
    }

    // roles = "PATIENT", NOT "ROLE_PATIENT" - the annotation adds the prefix
    @Test
    @WithMockUser(username = EMAIL, roles = "PATIENT")
    void createProfile_returns201_withALocationHeader_andTheStandardEnvelope() throws Exception {
        when(patientService.createProfile(eq(EMAIL), any())).thenReturn(buildResponseDto());

        mockMvc.perform(post("/api/patients/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/patients/1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Patient profile created"))
                .andExpect(jsonPath("$.data.id").value(1))
                // the four flattened fields, proving the mapper's contract
                .andExpect(jsonPath("$.data.firstName").value("Rahul"))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.phone").value("9876543210"))
                .andExpect(jsonPath("$.data.gender").value("MALE"));
    }

    // the email passed to the service must be the AUTHENTICATED one,
    // and nothing in the body can influence it
    @Test
    @WithMockUser(username = EMAIL, roles = "PATIENT")
    void createProfile_passesTheAuthenticatedEmailToTheService() throws Exception {
        when(patientService.createProfile(eq(EMAIL), any())).thenReturn(buildResponseDto());

        Map<String, Object> bodyWithAnInjectedEmail = validBody();
        bodyWithAnInjectedEmail.put("email", "attacker@example.com");
        bodyWithAnInjectedEmail.put("userId", 99);

        mockMvc.perform(post("/api/patients/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWithAnInjectedEmail)))
                .andExpect(status().isCreated());

        // the extra keys were ignored: the DTO has no such fields
        verify(patientService).createProfile(eq(EMAIL), any());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "PATIENT")
    void createProfile_withABadGender_returns400_andTheServiceIsNeverCalled() throws Exception {
        Map<String, Object> body = validBody();
        body.put("gender", "male");

        mockMvc.perform(post("/api/patients/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // @Valid rejected it before the controller body ran at all
        verify(patientService, never()).createProfile(any(), any());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "PATIENT")
    void getMyProfile_returns200AndTheProfile() throws Exception {
        when(patientService.getMyProfile(EMAIL)).thenReturn(buildResponseDto());

        mockMvc.perform(get("/api/patients/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Patient profile retrieved"))
                .andExpect(jsonPath("$.data.email").value(EMAIL));
    }

    // no @WithMockUser: an anonymous request
    @Test
    void getMyProfile_withoutAToken_returns401() throws Exception {
        mockMvc.perform(get("/api/patients/profile"))
                .andExpect(status().isUnauthorized());

        verify(patientService, never()).getMyProfile(any());
    }

    // authenticated, but with the wrong role: 403, not 401
    @Test
    @WithMockUser(username = "priya@example.com", roles = "DOCTOR")
    void createProfile_asADoctor_returns403() throws Exception {
        mockMvc.perform(post("/api/patients/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isForbidden());

        verify(patientService, never()).createProfile(any(), any());
    }
}