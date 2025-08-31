package org.games.matchmakingservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringConfigTest {

    private MonitoringConfig monitoringConfig;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        monitoringConfig = new MonitoringConfig();
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void matchmakingProcessingTimer_CreatesTimerWithCorrectName() {
        Timer timer = monitoringConfig.matchmakingProcessingTimer(meterRegistry);
        
        assertNotNull(timer);
        assertEquals("matchmaking.processing.time", timer.getId().getName());
        assertEquals("Time taken to process matchmaking queue", timer.getId().getDescription());
    }

    @Test
    void matchCreationTimer_CreatesTimerWithCorrectName() {
        Timer timer = monitoringConfig.matchCreationTimer(meterRegistry);
        
        assertNotNull(timer);
        assertEquals("matchmaking.match.creation.time", timer.getId().getName());
        assertEquals("Time taken to create a match", timer.getId().getDescription());
    }

    @Test
    void enqueueSuccessCounter_CreatesCounterWithCorrectName() {
        Counter counter = monitoringConfig.enqueueSuccessCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("matchmaking.enqueue.success", counter.getId().getName());
        assertEquals("Number of successful player enqueues", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void enqueueFailureCounter_CreatesCounterWithCorrectName() {
        Counter counter = monitoringConfig.enqueueFailureCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("matchmaking.enqueue.failure", counter.getId().getName());
        assertEquals("Number of failed player enqueues", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void dequeueSuccessCounter_CreatesCounterWithCorrectName() {
        Counter counter = monitoringConfig.dequeueSuccessCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("matchmaking.dequeue.success", counter.getId().getName());
        assertEquals("Number of successful player dequeues", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void dequeueFailureCounter_CreatesCounterWithCorrectName() {
        Counter counter = monitoringConfig.dequeueFailureCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("matchmaking.dequeue.failure", counter.getId().getName());
        assertEquals("Number of failed player dequeues", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void websocketConnectionCounter_CreatesCounterWithCorrectName() {
        Counter counter = monitoringConfig.websocketConnectionCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("websocket.connections", counter.getId().getName());
        assertEquals("Number of WebSocket connections established", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void websocketDisconnectionCounter_CreatesCounterWithCorrectName() {
        Counter counter = monitoringConfig.websocketDisconnectionCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("websocket.disconnections", counter.getId().getName());
        assertEquals("Number of WebSocket disconnections", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void playerWaitTimeTimer_CreatesTimerWithCorrectName() {
        Timer timer = monitoringConfig.playerWaitTimeTimer(meterRegistry);
        
        assertNotNull(timer);
        assertEquals("matchmaking.player.wait.time", timer.getId().getName());
        assertEquals("Time players spend waiting in queue before being matched", timer.getId().getDescription());
    }

    @Test
    void matchOutcomeCounter_CreatesCounterWithCorrectName() {
        Counter counter = monitoringConfig.matchOutcomeCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("matchmaking.matches.outcome", counter.getId().getName());
        assertEquals("Number of matches by outcome", counter.getId().getDescription());
        assertEquals(0.0, counter.count());
    }

    @Test
    void allMetricsAreRegisteredInMeterRegistry() {
        // Create all metrics
        Timer processingTimer = monitoringConfig.matchmakingProcessingTimer(meterRegistry);
        Timer creationTimer = monitoringConfig.matchCreationTimer(meterRegistry);
        Counter enqueueSuccess = monitoringConfig.enqueueSuccessCounter(meterRegistry);
        Counter enqueueFailure = monitoringConfig.enqueueFailureCounter(meterRegistry);
        Counter dequeueSuccess = monitoringConfig.dequeueSuccessCounter(meterRegistry);
        Counter dequeueFailure = monitoringConfig.dequeueFailureCounter(meterRegistry);
        Counter websocketConn = monitoringConfig.websocketConnectionCounter(meterRegistry);
        Counter websocketDisconn = monitoringConfig.websocketDisconnectionCounter(meterRegistry);
        Timer waitTimeTimer = monitoringConfig.playerWaitTimeTimer(meterRegistry);
        Counter matchOutcome = monitoringConfig.matchOutcomeCounter(meterRegistry);

        // Verify they're all registered in the meter registry
        assertTrue(meterRegistry.getMeters().contains(processingTimer));
        assertTrue(meterRegistry.getMeters().contains(creationTimer));
        assertTrue(meterRegistry.getMeters().contains(enqueueSuccess));
        assertTrue(meterRegistry.getMeters().contains(enqueueFailure));
        assertTrue(meterRegistry.getMeters().contains(dequeueSuccess));
        assertTrue(meterRegistry.getMeters().contains(dequeueFailure));
        assertTrue(meterRegistry.getMeters().contains(websocketConn));
        assertTrue(meterRegistry.getMeters().contains(websocketDisconn));
        assertTrue(meterRegistry.getMeters().contains(waitTimeTimer));
        assertTrue(meterRegistry.getMeters().contains(matchOutcome));
        
        // Should have exactly 10 metrics
        assertEquals(10, meterRegistry.getMeters().size());
    }

    @Test
    void countersCanBeIncremented() {
        Counter enqueueSuccess = monitoringConfig.enqueueSuccessCounter(meterRegistry);
        Counter enqueueFailure = monitoringConfig.enqueueFailureCounter(meterRegistry);
        
        assertEquals(0.0, enqueueSuccess.count());
        assertEquals(0.0, enqueueFailure.count());
        
        enqueueSuccess.increment();
        enqueueFailure.increment(5);
        
        assertEquals(1.0, enqueueSuccess.count());
        assertEquals(5.0, enqueueFailure.count());
    }

    @Test
    void timersCanRecordTime() {
        Timer processingTimer = monitoringConfig.matchmakingProcessingTimer(meterRegistry);
        
        assertEquals(0, processingTimer.count());
        assertEquals(0.0, processingTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        
        processingTimer.record(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        processingTimer.record(200, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        assertEquals(2, processingTimer.count());
        assertEquals(300.0, processingTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        assertEquals(150.0, processingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
    }
}
