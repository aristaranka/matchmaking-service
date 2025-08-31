package org.games.matchmakingservice.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.games.matchmakingservice.service.MatchmakingService;
import org.games.matchmakingservice.service.WebSocketConnectionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Controller providing comprehensive monitoring and metrics endpoints.
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MonitoringController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringController.class);

    private final MatchmakingService matchmakingService;
    private final WebSocketConnectionTracker connectionTracker;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    private static final String MATCHMAKING_QUEUE = "matchmaking:queue";
    private static final String MATCHMAKING_REQUESTS = "matchmaking:requests";
    private static final String MATCHMAKING_RESULTS = "matchmaking:results";

    /**
     * Get comprehensive system metrics.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Matchmaking metrics
            Map<String, Object> matchmakingMetrics = new HashMap<>();
            matchmakingMetrics.put("enabled", matchmakingService.isMatchmakingEnabled());
            matchmakingMetrics.put("queueSize", getQueueSize());
            matchmakingMetrics.put("pendingRequests", getPendingRequestsCount());
            matchmakingMetrics.put("totalMatches", getCounterValue("matchmaking.matches.made"));
            matchmakingMetrics.put("successfulEnqueues", getCounterValue("matchmaking.enqueue.success"));
            matchmakingMetrics.put("failedEnqueues", getCounterValue("matchmaking.enqueue.failure"));
            matchmakingMetrics.put("successfulDequeues", getCounterValue("matchmaking.dequeue.success"));
            matchmakingMetrics.put("failedDequeues", getCounterValue("matchmaking.dequeue.failure"));
            
            metrics.put("matchmaking", matchmakingMetrics);
            
            // WebSocket metrics
            Map<String, Object> websocketMetrics = new HashMap<>();
            websocketMetrics.put("activeConnections", connectionTracker.getConnectionCount());
            websocketMetrics.put("hasActiveConnections", connectionTracker.hasActiveConnections());
            websocketMetrics.put("totalConnections", getCounterValue("websocket.connections"));
            websocketMetrics.put("totalDisconnections", getCounterValue("websocket.disconnections"));
            websocketMetrics.put("connectionDetails", connectionTracker.getActiveConnections());
            
            metrics.put("websocket", websocketMetrics);
            
            // System metrics
            metrics.put("system", getSystemMetrics());
            
            // Redis metrics
            metrics.put("redis", getRedisMetrics());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.error("Error retrieving metrics", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to retrieve metrics",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get queue statistics and analysis.
     */
    @GetMapping("/queue/stats")
    public ResponseEntity<Map<String, Object>> getQueueStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            Long queueSize = getQueueSize();
            Long pendingRequests = getPendingRequestsCount();
            
            stats.put("currentSize", queueSize);
            stats.put("pendingRequests", pendingRequests);
            stats.put("consistency", queueSize.equals(pendingRequests) ? "consistent" : "inconsistent");
            
            // Get queue age statistics (oldest and newest entries)
            if (queueSize > 0) {
                // Get oldest entry (lowest score)
                var oldestEntry = redisTemplate.opsForZSet().rangeWithScores(MATCHMAKING_QUEUE, 0, 0);
                // Get newest entry (highest score)
                var newestEntry = redisTemplate.opsForZSet().reverseRangeWithScores(MATCHMAKING_QUEUE, 0, 0);
                
                if (!oldestEntry.isEmpty() && !newestEntry.isEmpty()) {
                    stats.put("oldestPlayerScore", oldestEntry.iterator().next().getScore());
                    stats.put("newestPlayerScore", newestEntry.iterator().next().getScore());
                }
            }
            
            // Performance metrics
            Timer processingTimer = meterRegistry.find("matchmaking.processing.time").timer();
            if (processingTimer != null) {
                stats.put("averageProcessingTime", processingTimer.mean(TimeUnit.MILLISECONDS));
                stats.put("maxProcessingTime", processingTimer.max(TimeUnit.MILLISECONDS));
                stats.put("totalProcessingCalls", processingTimer.count());
            }
            
            Timer waitTimeTimer = meterRegistry.find("matchmaking.player.wait.time").timer();
            if (waitTimeTimer != null) {
                stats.put("averageWaitTime", waitTimeTimer.mean(TimeUnit.SECONDS));
                stats.put("maxWaitTime", waitTimeTimer.max(TimeUnit.SECONDS));
                stats.put("totalWaitTimeMeasurements", waitTimeTimer.count());
            }
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving queue stats", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to retrieve queue stats",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get system health summary.
     */
    @GetMapping("/health/summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Overall status
            boolean isHealthy = true;
            StringBuilder issues = new StringBuilder();
            
            // Check matchmaking service
            boolean matchmakingEnabled = matchmakingService.isMatchmakingEnabled();
            health.put("matchmakingEnabled", matchmakingEnabled);
            
            // Check Redis connectivity
            boolean redisHealthy = checkRedisHealth();
            health.put("redisHealthy", redisHealthy);
            if (!redisHealthy) {
                isHealthy = false;
                issues.append("Redis connectivity issues; ");
            }
            
            // Check queue health
            Long queueSize = getQueueSize();
            boolean queueHealthy = queueSize < 1000; // Threshold for queue health
            health.put("queueHealthy", queueHealthy);
            health.put("queueSize", queueSize);
            if (!queueHealthy) {
                isHealthy = false;
                issues.append("Queue size critically high; ");
            }
            
            // Check WebSocket connections
            boolean hasConnections = connectionTracker.hasActiveConnections();
            health.put("hasActiveWebSocketConnections", hasConnections);
            health.put("activeConnections", connectionTracker.getConnectionCount());
            
            // Overall health determination
            health.put("overall", isHealthy ? "HEALTHY" : "UNHEALTHY");
            if (!isHealthy) {
                health.put("issues", issues.toString());
            }
            
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Error retrieving health summary", e);
            return ResponseEntity.status(500).body(Map.of(
                "overall", "ERROR",
                "error", "Failed to retrieve health summary",
                "message", e.getMessage()
            ));
        }
    }

    // Helper methods
    private Long getQueueSize() {
        try {
            Long size = redisTemplate.opsForZSet().zCard(MATCHMAKING_QUEUE);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.warn("Failed to get queue size", e);
            return 0L;
        }
    }

    private Long getPendingRequestsCount() {
        try {
            Long count = redisTemplate.opsForHash().size(MATCHMAKING_REQUESTS);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Failed to get pending requests count", e);
            return 0L;
        }
    }

    private double getCounterValue(String counterName) {
        Counter counter = meterRegistry.find(counterName).counter();
        return counter != null ? counter.count() : 0.0;
    }

    private boolean checkRedisHealth() {
        try {
            if (redisTemplate.getConnectionFactory() == null) {
                return false;
            }
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return "PONG".equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> system = new HashMap<>();
        
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        system.put("uptime", runtimeBean.getUptime());
        system.put("startTime", runtimeBean.getStartTime());
        
        // Memory metrics
        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
        memory.put("heapCommitted", memoryBean.getHeapMemoryUsage().getCommitted());
        memory.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
        memory.put("nonHeapMax", memoryBean.getNonHeapMemoryUsage().getMax());
        
        system.put("memory", memory);
        
        // CPU and thread info
        system.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        system.put("activeThreads", Thread.activeCount());
        
        return system;
    }

    private Map<String, Object> getRedisMetrics() {
        Map<String, Object> redis = new HashMap<>();
        
        try {
            if (redisTemplate.getConnectionFactory() == null) {
                redis.put("error", "Redis connection factory is null");
                redis.put("connected", false);
                return redis;
            }
            
            // Basic connectivity
            String pingResult = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            redis.put("ping", pingResult);
            redis.put("connected", "PONG".equals(pingResult));
            
            // Key existence checks for our specific keys
            redis.put("queueKeyExists", redisTemplate.hasKey(MATCHMAKING_QUEUE));
            redis.put("requestsKeyExists", redisTemplate.hasKey(MATCHMAKING_REQUESTS));
            redis.put("resultsKeyExists", redisTemplate.hasKey(MATCHMAKING_RESULTS));
            
        } catch (Exception e) {
            redis.put("error", e.getMessage());
            redis.put("connected", false);
        }
        
        return redis;
    }
}
