package org.games.matchmakingservice.dto;

import java.time.LocalDateTime;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private LocalDateTime expiresAt;
    private String message;
    
    // Constructors
    public AuthResponse() {}
    
    public AuthResponse(String token, String username, String role, LocalDateTime expiresAt) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
    }
    
    public AuthResponse(String message) {
        this.message = message;
    }
    
    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
