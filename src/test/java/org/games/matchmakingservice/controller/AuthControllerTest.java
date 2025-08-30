package org.games.matchmakingservice.controller;

import org.games.matchmakingservice.config.JwtService;
import org.games.matchmakingservice.domain.User;
import org.games.matchmakingservice.dto.AuthResponse;
import org.games.matchmakingservice.dto.RegisterRequest;
import org.games.matchmakingservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_WithValidData_ReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(User.Role.USER);

        when(userService.createUser("testuser", "password123", "test@example.com")).thenReturn(user);
        when(jwtService.generateToken("testuser", Map.of("role", "USER", "userId", 1L))).thenReturn("fake.jwt.token");
        when(jwtService.getExpirationTime("fake.jwt.token")).thenReturn(LocalDateTime.now().plusHours(24));

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertEquals(200, response.getStatusCode().value());
        AuthResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("fake.jwt.token", body.getToken());
        assertEquals("testuser", body.getUsername());
        assertEquals("USER", body.getRole());
    }

    @Test
    void register_WithExistingUsername_ReturnsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        when(userService.createUser("existinguser", "password123", "test@example.com"))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertEquals(400, response.getStatusCode().value());
        AuthResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Username already exists", body.getMessage());
    }
}


