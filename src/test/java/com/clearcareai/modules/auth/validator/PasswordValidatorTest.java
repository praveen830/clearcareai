package com.clearcareai.modules.auth.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        // a plain object - Hibernate Validator would create it the same way
        validator = new PasswordValidator();
    }

    @Test
    void validPassword_passes() {
        assertTrue(validator.isValid("Password123", null));
    }

    @Test
    void exactlyEightCharacters_passes() {
        // the boundary: 8 is allowed, 7 is not
        assertTrue(validator.isValid("Passwo1d", null));
    }

    @Test
    void sevenCharacters_fails() {
        assertFalse(validator.isValid("Passw1d", null));
    }

    @Test
    void noUppercase_fails() {
        assertFalse(validator.isValid("password123", null));
    }

    @Test
    void noLowercase_fails() {
        assertFalse(validator.isValid("PASSWORD123", null));
    }

    @Test
    void noDigit_fails() {
        assertFalse(validator.isValid("PasswordOnly", null));
    }

    @Test
    void null_passes_becauseNotBlankOwnsThatRule() {
        // deliberate: every non-presence constraint treats null as valid,
        // so a missing password produces one message, not two
        assertTrue(validator.isValid(null, null));
    }
}