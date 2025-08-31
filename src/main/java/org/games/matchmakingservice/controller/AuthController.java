package org.games.matchmakingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.games.matchmakingservice.config.JwtService;
import org.games.matchmakingservice.domain.User;
import org.games.matchmakingservice.dto.AuthResponse;
import org.games.matchmakingservice.dto.LoginRequest;
import org.games.matchmakingservice.dto.RegisterRequest;
import org.games.matchmakingservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "Authentication", description = "User registration, login, and JWT token management")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtService jwtService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtService jwtService, UserService userService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @Operation(
        summary = "Register a new user",
        description = "Create a new user account with username, email, and password. Returns JWT token upon successful registration."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Successful Registration",
                    value = """
                    {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "username": "player1",
                        "role": "USER",
                        "expiresAt": "2024-01-02T12:00:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or user already exists",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Registration Error",
                    value = """
                    {
                        "message": "Username already exists"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                    {
                        "message": "Registration failed due to server error"
                    }
                    """
                )
            )
        )
    })
    @PostMapping(value = "/register", produces = {"application/json", "*/*"})
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Register endpoint called for username: {}", request.getUsername());
            
            User user = userService.createUser(request.getUsername(), request.getPassword(), request.getEmail());
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().name());
            claims.put("userId", user.getId());
            
            String token = jwtService.generateToken(user.getUsername(), claims);
            LocalDateTime expiresAt = jwtService.getExpirationTime(token);
            
            log.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name(), expiresAt));
        } catch (IllegalArgumentException e) {
            log.error("Registration failed with IllegalArgumentException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new AuthResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration failed with exception: ", e);
            return ResponseEntity.status(500).body(new AuthResponse("Registration failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "User login",
        description = "Authenticate user with username and password. Returns JWT token upon successful login."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Successful Login",
                    value = """
                    {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "username": "player1",
                        "role": "USER",
                        "expiresAt": "2024-01-02T12:00:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Credentials",
                    value = """
                    {
                        "message": "Invalid username or password"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    @PostMapping(value = "/login", produces = {"application/json", "*/*"})
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login endpoint called for username: {}", request.getUsername());
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = (User) authentication.getPrincipal();
            userService.updateLastLogin(user.getUsername());
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole().name());
            claims.put("userId", user.getId());
            
            String token = jwtService.generateToken(user.getUsername(), claims);
            LocalDateTime expiresAt = jwtService.getExpirationTime(token);
            
            log.info("User logged in successfully: {}", user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name(), expiresAt));
        } catch (Exception e) {
            log.error("Login failed for username: {} with error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(new AuthResponse("Invalid username or password"));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!jwtService.isTokenExpired(token)) {
                    String subject = jwtService.getSubject(token);
                    LocalDateTime expiresAt = jwtService.getExpirationTime(token);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("username", subject);
                    response.put("expiresAt", expiresAt);
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.ok(Map.of("valid", false));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }
    

}


