package com.kloudspot.support.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigCorsTest {
    @Test
    void allowsLocalFrontendOriginsAcrossDevelopmentPortsOnly() {
        var source = new SecurityConfig().corsConfigurationSource();
        var request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertEquals("http://localhost:5173", configuration.checkOrigin("http://localhost:5173"));
        assertEquals("http://localhost:5174", configuration.checkOrigin("http://localhost:5174"));
        assertEquals("http://127.0.0.1:5173", configuration.checkOrigin("http://127.0.0.1:5173"));
        assertNull(configuration.checkOrigin("https://example.com"));
    }
}
