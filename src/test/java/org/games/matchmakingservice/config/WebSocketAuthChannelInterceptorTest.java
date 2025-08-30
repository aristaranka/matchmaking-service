package org.games.matchmakingservice.config;

import org.games.matchmakingservice.service.WebSocketConnectionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private WebSocketConnectionTracker connectionTracker;

    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new WebSocketAuthChannelInterceptor(jwtService, connectionTracker);
    }

    @Test
    void preSend_SetsUserOnValidToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtService.getSubject("abc.def.ghi")).thenReturn("alice");

        Message<?> out = interceptor.preSend(message, new MessageChannel() {
            @Override
            public boolean send(Message<?> message) { return true; }
            @Override
            public boolean send(Message<?> message, long timeout) { return true; }
        });

        StompHeaderAccessor outAcc = StompHeaderAccessor.wrap(out);
        assertNotNull(outAcc.getUser());
        assertEquals("alice", outAcc.getUser().getName());
    }
}


