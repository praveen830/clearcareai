package com.clearcareai.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// same package as the filter, so the protected shouldNotFilter is reachable
class RequestLoggingFilterTest {

    private RequestLoggingFilter requestLoggingFilter;

    @BeforeEach
    void setUp() {
        requestLoggingFilter = new RequestLoggingFilter();
    }

    @Test
    void filter_passesTheRequestFurtherDownTheChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        requestLoggingFilter.doFilter(request, response, filterChain);

        // MockFilterChain records what it was called with - a null here would
        // mean the filter swallowed the request, which is the worst bug a
        // filter can have (no response, client hangs, nothing logged)
        assertNotNull(filterChain.getRequest());
    }

    @Test
    void shouldNotFilter_skipsInfrastructurePathsButNotApiTraffic() throws Exception {
        assertTrue(requestLoggingFilter.shouldNotFilter(
                new MockHttpServletRequest("GET", "/actuator/health")));
        assertTrue(requestLoggingFilter.shouldNotFilter(
                new MockHttpServletRequest("GET", "/swagger-ui/index.html")));
        assertTrue(requestLoggingFilter.shouldNotFilter(
                new MockHttpServletRequest("GET", "/api-docs")));

        // real traffic must still be logged
        assertFalse(requestLoggingFilter.shouldNotFilter(
                new MockHttpServletRequest("POST", "/api/auth/login")));
    }
}