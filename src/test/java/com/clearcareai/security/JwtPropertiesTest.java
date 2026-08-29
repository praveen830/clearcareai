package com.clearcareai.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class JwtPropertiesTest {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void properties_bindFromYaml() {
        assertNotNull(jwtProperties.getSecret());

        // 24 hours and 7 days, from COMMANDO section 10
        assertEquals(86400000L, jwtProperties.getExpiration());
        assertEquals(604800000L, jwtProperties.getRefreshExpiration());
    }

    @Test
    void secret_isLongEnoughForHs256() {
        // 32 characters is 256 bits, which is what HS256 requires
        assertTrue(jwtProperties.getSecret().length() >= 32);
    }
}