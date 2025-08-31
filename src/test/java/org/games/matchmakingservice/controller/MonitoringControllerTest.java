package org.games.matchmakingservice.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.games.matchmakingservice.service.MatchmakingService;
import org.games.matchmakingservice.service.WebSocketConnectionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringControllerTest {

    @Mock
    private MatchmakingService matchmakingService;

    @Mock
    private WebSocketConnectionTracker connectionTracker;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private MeterRegistry meterRegistry;

    @InjectMocks
    private MonitoringController monitoringController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        monitoringController = new MonitoringController(
                matchmakingService, 
                connectionTracker, 
                redisTemplate, 
                meterRegistry
        );

        // Setup Redis template mocks
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
    }

    @Test
    void getMetrics_Success() {
        // Arrange
        when(matchmakingService.isMatchmakingEnabled()).thenReturn(true);
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(5L);
        when(hashOperations.size("matchmaking:requests")).thenReturn(3L);
        when(connectionTracker.getConnectionCount()).thenReturn(10);
        when(connectionTracker.hasActiveConnections()).thenReturn(true);
        when(connectionTracker.getActiveConnections()).thenReturn(createMockConnections());
        when(redisConnection.ping()).thenReturn("PONG");
        when(redisTemplate.hasKey("matchmaking:queue")).thenReturn(true);
        when(redisTemplate.hasKey("matchmaking:requests")).thenReturn(true);
        when(redisTemplate.hasKey("matchmaking:results")).thenReturn(true);

        // Create some test metrics
        Counter.builder("matchmaking.enqueue.success").register(meterRegistry).increment(10);
        Counter.builder("matchmaking.enqueue.failure").register(meterRegistry).increment(2);
        Counter.builder("websocket.connections").register(meterRegistry).increment(15);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getMetrics();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("matchmaking"));
        assertTrue(body.containsKey("websocket"));
        assertTrue(body.containsKey("system"));
        assertTrue(body.containsKey("redis"));

        // Check matchmaking metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> matchmakingMetrics = (Map<String, Object>) body.get("matchmaking");
        assertEquals(true, matchmakingMetrics.get("enabled"));
        assertEquals(5L, matchmakingMetrics.get("queueSize"));
        assertEquals(3L, matchmakingMetrics.get("pendingRequests"));
        assertEquals(10.0, matchmakingMetrics.get("successfulEnqueues"));
        assertEquals(2.0, matchmakingMetrics.get("failedEnqueues"));

        // Check websocket metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> websocketMetrics = (Map<String, Object>) body.get("websocket");
        assertEquals(10, websocketMetrics.get("activeConnections"));
        assertEquals(true, websocketMetrics.get("hasActiveConnections"));
        assertEquals(15.0, websocketMetrics.get("totalConnections"));
    }

    @Test
    void getMetrics_RedisFailure_ReturnsPartialMetrics() {
        // Arrange
        when(matchmakingService.isMatchmakingEnabled()).thenReturn(true);
        when(zSetOperations.zCard("matchmaking:queue")).thenThrow(new RuntimeException("Redis error"));
        when(connectionTracker.getConnectionCount()).thenReturn(5);
        when(connectionTracker.hasActiveConnections()).thenReturn(true);
        when(connectionTracker.getActiveConnections()).thenReturn(createMockConnections());

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getMetrics();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> matchmakingMetrics = (Map<String, Object>) body.get("matchmaking");
        assertEquals(0L, matchmakingMetrics.get("queueSize")); // Should default to 0 on error
    }

    @Test
    void getMetrics_GeneralException_ReturnsError() {
        // Arrange
        when(matchmakingService.isMatchmakingEnabled()).thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getMetrics();

        // Assert
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Failed to retrieve metrics", body.get("error"));
        assertEquals("Service error", body.get("message"));
    }

    @Test
    void getQueueStats_Success() {
        // Arrange
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(10L);
        when(hashOperations.size("matchmaking:requests")).thenReturn(10L);

        // Mock queue entries for age statistics
        Set<ZSetOperations.TypedTuple<Object>> oldestEntry = new HashSet<>();
        oldestEntry.add(ZSetOperations.TypedTuple.of("player1", 1000.0));
        when(zSetOperations.rangeWithScores("matchmaking:queue", 0, 0)).thenReturn(oldestEntry);

        Set<ZSetOperations.TypedTuple<Object>> newestEntry = new HashSet<>();
        newestEntry.add(ZSetOperations.TypedTuple.of("player2", 2000.0));
        when(zSetOperations.reverseRangeWithScores("matchmaking:queue", 0, 0)).thenReturn(newestEntry);

        // Create test timers
        Timer processingTimer = Timer.builder("matchmaking.processing.time").register(meterRegistry);
        processingTimer.record(100, TimeUnit.MILLISECONDS);
        processingTimer.record(200, TimeUnit.MILLISECONDS);

        Timer waitTimeTimer = Timer.builder("matchmaking.player.wait.time").register(meterRegistry);
        waitTimeTimer.record(5, TimeUnit.SECONDS);
        waitTimeTimer.record(10, TimeUnit.SECONDS);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getQueueStats();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(10L, body.get("currentSize"));
        assertEquals(10L, body.get("pendingRequests"));
        assertEquals("consistent", body.get("consistency"));
        assertEquals(1000.0, body.get("oldestPlayerScore"));
        assertEquals(2000.0, body.get("newestPlayerScore"));
        assertTrue(body.containsKey("averageProcessingTime"));
        assertTrue(body.containsKey("averageWaitTime"));
    }

    @Test
    void getQueueStats_InconsistentQueue_ReportsInconsistency() {
        // Arrange
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(5L);
        when(hashOperations.size("matchmaking:requests")).thenReturn(3L);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getQueueStats();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(5L, body.get("currentSize"));
        assertEquals(3L, body.get("pendingRequests"));
        assertEquals("inconsistent", body.get("consistency"));
    }

    @Test
    void getQueueStats_EmptyQueue_HandlesGracefully() {
        // Arrange
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(0L);
        when(hashOperations.size("matchmaking:requests")).thenReturn(0L);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getQueueStats();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(0L, body.get("currentSize"));
        assertEquals(0L, body.get("pendingRequests"));
        assertEquals("consistent", body.get("consistency"));
        assertFalse(body.containsKey("oldestPlayerScore"));
        assertFalse(body.containsKey("newestPlayerScore"));
    }

    @Test
    void getQueueStats_Exception_ReturnsError() {
        // Arrange
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(5L);
        when(hashOperations.size("matchmaking:requests")).thenReturn(5L);
        // Make the rangeWithScores call throw an exception to trigger the outer catch block
        when(zSetOperations.rangeWithScores("matchmaking:queue", 0, 0)).thenThrow(new RuntimeException("Redis error"));

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getQueueStats();

        // Assert
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Failed to retrieve queue stats", body.get("error"));
        assertEquals("Redis error", body.get("message"));
    }

    @Test
    void getHealthSummary_HealthySystem_ReturnsHealthy() {
        // Arrange
        when(matchmakingService.isMatchmakingEnabled()).thenReturn(true);
        when(redisConnection.ping()).thenReturn("PONG");
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(50L);
        when(connectionTracker.hasActiveConnections()).thenReturn(true);
        when(connectionTracker.getConnectionCount()).thenReturn(10);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthSummary();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("HEALTHY", body.get("overall"));
        assertEquals(true, body.get("matchmakingEnabled"));
        assertEquals(true, body.get("redisHealthy"));
        assertEquals(true, body.get("queueHealthy"));
        assertEquals(50L, body.get("queueSize"));
        assertEquals(true, body.get("hasActiveWebSocketConnections"));
        assertEquals(10, body.get("activeConnections"));
        assertFalse(body.containsKey("issues"));
        assertTrue(body.containsKey("timestamp"));
    }

    @Test
    void getHealthSummary_UnhealthySystem_ReturnsUnhealthy() {
        // Arrange
        when(matchmakingService.isMatchmakingEnabled()).thenReturn(true);
        when(redisConnection.ping()).thenThrow(new RuntimeException("Redis down"));
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(1500L); // Over threshold
        when(connectionTracker.hasActiveConnections()).thenReturn(false);
        when(connectionTracker.getConnectionCount()).thenReturn(0);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthSummary();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UNHEALTHY", body.get("overall"));
        assertEquals(true, body.get("matchmakingEnabled"));
        assertEquals(false, body.get("redisHealthy"));
        assertEquals(false, body.get("queueHealthy"));
        assertEquals(1500L, body.get("queueSize"));
        assertEquals(false, body.get("hasActiveWebSocketConnections"));
        assertEquals(0, body.get("activeConnections"));
        assertTrue(body.containsKey("issues"));
        assertTrue(((String) body.get("issues")).contains("Redis connectivity issues"));
        assertTrue(((String) body.get("issues")).contains("Queue size critically high"));
    }

    @Test
    void getHealthSummary_Exception_ReturnsError() {
        // Arrange
        when(matchmakingService.isMatchmakingEnabled()).thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthSummary();

        // Assert
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.get("overall"));
        assertEquals("Failed to retrieve health summary", body.get("error"));
        assertEquals("Service error", body.get("message"));
    }

    @Test
    void getHealthSummary_RedisConnectionFactoryNull_HandlesGracefully() {
        // Arrange
        when(matchmakingService.isMatchmakingEnabled()).thenReturn(true);
        when(redisTemplate.getConnectionFactory()).thenReturn(null);
        when(zSetOperations.zCard("matchmaking:queue")).thenReturn(10L);
        when(connectionTracker.hasActiveConnections()).thenReturn(true);
        when(connectionTracker.getConnectionCount()).thenReturn(5);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthSummary();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UNHEALTHY", body.get("overall"));
        assertEquals(false, body.get("redisHealthy"));
        assertTrue(((String) body.get("issues")).contains("Redis connectivity issues"));
    }

    private ConcurrentHashMap<String, String> createMockConnections() {
        ConcurrentHashMap<String, String> connections = new ConcurrentHashMap<>();
        connections.put("session1", "user1");
        connections.put("session2", "user2");
        return connections;
    }
}
