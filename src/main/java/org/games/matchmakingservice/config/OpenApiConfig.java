package org.games.matchmakingservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI 3.0 configuration for the Matchmaking Service.
 * Provides comprehensive API documentation with security schemes and examples.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Matchmaking Service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .tags(tagList())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Matchmaking Service API")
                .version("1.0.0")
                .description("""
                        # Matchmaking Service API
                        
                        A high-performance, real-time matchmaking service with Elo-based skill matching.
                        
                        ## Features
                        - 🎯 **Elo-based Matchmaking**: Intelligent skill-based player matching
                        - ⚡ **Real-time Communication**: WebSocket support for instant notifications
                        - 🔐 **JWT Authentication**: Secure user authentication and authorization
                        - 📊 **Player Statistics**: Comprehensive Elo tracking and leaderboards
                        - 📈 **Monitoring**: Built-in metrics and health checks
                        
                        ## Authentication
                        This API uses JWT (JSON Web Tokens) for authentication. Include the token in the Authorization header:
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ## WebSocket Integration
                        Connect to `/ws-match` endpoint for real-time match notifications and queue updates.
                        
                        ## Rate Limiting
                        - Authentication endpoints: 5 requests/second
                        - General API endpoints: 10 requests/second
                        - No rate limiting on health checks
                        """)
                .contact(new Contact()
                        .name("Matchmaking Service Team")
                        .email("support@matchmaking.example.com")
                        .url("https://github.com/your-org/matchmaking-service"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> serverList() {
        return Arrays.asList(
                new Server()
                        .url("http://localhost:8080")
                        .description("Development Server"),
                new Server()
                        .url("https://api.matchmaking.example.com")
                        .description("Production Server"),
                new Server()
                        .url("https://staging-api.matchmaking.example.com")
                        .description("Staging Server")
        );
    }

    private List<Tag> tagList() {
        return Arrays.asList(
                new Tag()
                        .name("Authentication")
                        .description("User registration, login, and JWT token management"),
                new Tag()
                        .name("Matchmaking")
                        .description("Core matchmaking operations - join/leave queue, match results"),
                new Tag()
                        .name("Player Stats")
                        .description("Player statistics, Elo ratings, and leaderboards"),
                new Tag()
                        .name("Monitoring")
                        .description("System metrics, queue status, and health checks"),
                new Tag()
                        .name("WebSocket")
                        .description("Real-time communication endpoints and connection management"),
                new Tag()
                        .name("Admin")
                        .description("Administrative operations - pause/resume matchmaking")
        );
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("bearerAuth")
                .description("JWT Authentication. Format: `Bearer <token>`");
    }
}
