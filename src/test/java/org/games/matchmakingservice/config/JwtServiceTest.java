package org.games.matchmakingservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-which-is-long-enough-1234567890");
        ReflectionTestUtils.setField(jwtService, "ttlSeconds", 3600L);
    }

    @Test
    void generateAndParseToken_Success() {
        String token = jwtService.generateToken("alice", Map.of("role", "USER"));
        assertNotNull(token);
        assertEquals("alice", jwtService.getSubject(token));
    }
}


