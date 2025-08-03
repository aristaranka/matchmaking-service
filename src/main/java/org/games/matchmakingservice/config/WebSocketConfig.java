package org.games.matchmakingservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.ws.endpoint}")
    private String wsEndpoint;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(wsEndpoint)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for sending messages to clients
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Set application destination prefix for messages from clients
        registry.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }
} 