package com.clearcareai.modules.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        // builds Hibernate Validator directly - no Spring context needed
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // a request with everything correct, which each test then breaks one field of
    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("rahul@example.com");
        request.setPassword("Password123");
        request.setFirstName("Rahul");
        request.setLastName("Sharma");
        request.setPhone("9876543210");
        request.setRole("ROLE_PATIENT");
        return request;
    }

    // helper: is there a violation on this field?
    private boolean hasViolationOn(Set<ConstraintViolation<RegisterRequest>> violations,
                                   String fieldName) {
        for (ConstraintViolation<RegisterRequest> violation : violations) {
            if (violation.getPropertyPath().toString().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void aFullyValidRequest_hasNoViolations() {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(validRequest());

        assertEquals(0, violations.size());
    }

    @Test
    void badEmail_isRejected() {
        RegisterRequest request = validRequest();
        request.setEmail("not-an-email");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolationOn(violations, "email"));
    }

    @Test
    void emptyEmail_isRejected_becauseEmailAloneWouldAllowIt() {
        RegisterRequest request = validRequest();
        request.setEmail("");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // @Email accepts "" - @NotBlank is what catches this
        assertTrue(hasViolationOn(violations, "email"));
    }

    @Test
    void weakPassword_isRejected_provingValidPasswordIsWiredUp() {
        RegisterRequest request = validRequest();
        request.setPassword("abc");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolationOn(violations, "password"));
    }

    @Test
    void phoneWithLetters_isRejected() {
        RegisterRequest request = validRequest();
        request.setPhone("98765abcde");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolationOn(violations, "phone"));
    }

    @Test
    void unknownRole_isRejected() {
        RegisterRequest request = validRequest();
        request.setRole("ROLE_WIZARD");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolationOn(violations, "role"));
    }
}