package com.clearcareai.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    void success_setsSuccessTrueAndCarriesData() {
        ApiResponse<String> response = ApiResponse.success("Doctor fetched", "some-payload");

        assertTrue(response.isSuccess());
        assertEquals("Doctor fetched", response.getMessage());
        assertEquals("some-payload", response.getData());
    }

    @Test
    void error_setsSuccessFalseAndLeavesDataNull() {
        ApiResponse<String> response = ApiResponse.error("Doctor not found");

        assertFalse(response.isSuccess());
        assertEquals("Doctor not found", response.getMessage());

        // deliberate: a failed response carries no payload
        assertNull(response.getData());
    }
}