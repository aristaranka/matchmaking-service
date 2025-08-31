package org.games.matchmakingservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to track active WebSocket connections.
 * Used to determine if matchmaking should proceed based on connected observers.
 */
@Service
public class WebSocketConnectionTracker {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionTracker.class);
    
    private final ConcurrentHashMap<String, String> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    
    private final Counter websocketConnectionCounter;
    private final Counter websocketDisconnectionCounter;
    private final MeterRegistry meterRegistry;

    public WebSocketConnectionTracker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.websocketConnectionCounter = Counter.builder("websocket.connections")
                .description("Number of WebSocket connections established")
                .register(meterRegistry);
        this.websocketDisconnectionCounter = Counter.builder("websocket.disconnections")
                .description("Number of WebSocket disconnections")
                .register(meterRegistry);
    }

    /**
     * Register a new WebSocket connection.
     * 
     * @param sessionId The session ID of the connection
     * @param username The username of the connected user
     */
    public void addConnection(String sessionId, String username) {
        activeConnections.put(sessionId, username);
        int count = connectionCount.incrementAndGet();
        
        // Update metrics
        websocketConnectionCounter.increment();
        meterRegistry.gauge("websocket.active.connections", count);
        
        log.info("WebSocket connection added: sessionId={}, username={}, total connections={}", 
                sessionId, username, count);
    }

    /**
     * Unregister a WebSocket connection.
     * 
     * @param sessionId The session ID of the connection
     */
    public void removeConnection(String sessionId) {
        String username = activeConnections.remove(sessionId);
        int count = connectionCount.decrementAndGet();
        
        // Update metrics
        websocketDisconnectionCounter.increment();
        meterRegistry.gauge("websocket.active.connections", count);
        
        log.info("WebSocket connection removed: sessionId={}, username={}, total connections={}", 
                sessionId, username, count);
    }

    /**
     * Check if there are any active WebSocket connections.
     * 
     * @return true if there are active connections, false otherwise
     */
    public boolean hasActiveConnections() {
        return connectionCount.get() > 0;
    }

    /**
     * Get the number of active WebSocket connections.
     * 
     * @return the number of active connections
     */
    public int getConnectionCount() {
        return connectionCount.get();
    }

    /**
     * Get all active connections (for debugging).
     * 
     * @return map of session IDs to usernames
     */
    public ConcurrentHashMap<String, String> getActiveConnections() {
        return new ConcurrentHashMap<>(activeConnections);
    }
}
