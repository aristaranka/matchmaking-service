package org.games.matchmakingservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for custom metrics and monitoring.
 */
@Configuration
public class MonitoringConfig {

    /**
     * Timer for measuring matchmaking processing time.
     */
    @Bean
    public Timer matchmakingProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("matchmaking.processing.time")
                .description("Time taken to process matchmaking queue")
                .register(meterRegistry);
    }

    /**
     * Timer for measuring match creation time.
     */
    @Bean
    public Timer matchCreationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("matchmaking.match.creation.time")
                .description("Time taken to create a match")
                .register(meterRegistry);
    }

    /**
     * Counter for successful enqueues.
     */
    @Bean
    public Counter enqueueSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("matchmaking.enqueue.success")
                .description("Number of successful player enqueues")
                .register(meterRegistry);
    }

    /**
     * Counter for failed enqueues.
     */
    @Bean
    public Counter enqueueFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("matchmaking.enqueue.failure")
                .description("Number of failed player enqueues")
                .register(meterRegistry);
    }

    /**
     * Counter for successful dequeues.
     */
    @Bean
    public Counter dequeueSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("matchmaking.dequeue.success")
                .description("Number of successful player dequeues")
                .register(meterRegistry);
    }

    /**
     * Counter for failed dequeues.
     */
    @Bean
    public Counter dequeueFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("matchmaking.dequeue.failure")
                .description("Number of failed player dequeues")
                .register(meterRegistry);
    }

    /**
     * Counter for WebSocket connections.
     */
    @Bean
    public Counter websocketConnectionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("websocket.connections")
                .description("Number of WebSocket connections established")
                .register(meterRegistry);
    }

    /**
     * Counter for WebSocket disconnections.
     */
    @Bean
    public Counter websocketDisconnectionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("websocket.disconnections")
                .description("Number of WebSocket disconnections")
                .register(meterRegistry);
    }

    /**
     * Timer for measuring player wait time in queue.
     */
    @Bean
    public Timer playerWaitTimeTimer(MeterRegistry meterRegistry) {
        return Timer.builder("matchmaking.player.wait.time")
                .description("Time players spend waiting in queue before being matched")
                .register(meterRegistry);
    }

    /**
     * Counter for matches by outcome.
     */
    @Bean
    public Counter matchOutcomeCounter(MeterRegistry meterRegistry) {
        return Counter.builder("matchmaking.matches.outcome")
                .description("Number of matches by outcome")
                .register(meterRegistry);
    }
}
