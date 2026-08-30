package com.clearcareai.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpoint_withoutAToken_returns401InOurJsonShape() throws Exception {
        mockMvc.perform(get("/api/patients/profile"))
                .andExpect(status().isUnauthorized())
                // proves AuthEntryPoint ran, not Spring's default error page
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void actuatorHealth_staysPublic() throws Exception {
        // Actuator's own security auto-config backs off once we define a
        // SecurityFilterChain, so this passes only because we listed it
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocs_staysPublic_soSwaggerKeepsWorking() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk());
    }
}