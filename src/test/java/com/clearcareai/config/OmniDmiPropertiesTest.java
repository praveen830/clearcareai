package com.clearcareai.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class OmniDimPropertiesTest {

    @Autowired
    private OmniDimProperties omniDimProperties;

    @Test
    void properties_bindFromYaml() {
        assertNotNull(omniDimProperties.getApiKey());

        // compared as a number, not a string - this would not compile if the
        // field were still a String, which is the point of the type change
        assertEquals(208102, omniDimProperties.getAgentId());

        assertEquals("https://backend.omnidim.io/api/v1", omniDimProperties.getBaseUrl());
    }
}