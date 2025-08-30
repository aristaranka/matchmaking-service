package org.games.matchmakingservice.config;

import org.games.matchmakingservice.service.WebSocketConnectionTracker;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final WebSocketConnectionTracker connectionTracker;

    public WebSocketAuthChannelInterceptor(JwtService jwtService, WebSocketConnectionTracker connectionTracker) {
        this.jwtService = jwtService;
        this.connectionTracker = connectionTracker;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String subject = jwtService.getSubject(token);
                    accessor.setUser(() -> subject);
                    
                    // Track the connection
                    String sessionId = accessor.getSessionId();
                    if (sessionId != null) {
                        connectionTracker.addConnection(sessionId, subject);
                    }
                } catch (Exception ignored) {
                }
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            // Track disconnection
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                connectionTracker.removeConnection(sessionId);
            }
        }
        
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}


