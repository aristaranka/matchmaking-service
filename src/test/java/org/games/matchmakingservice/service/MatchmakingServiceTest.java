package org.games.matchmakingservice.service;

import org.games.matchmakingservice.domain.MatchRequest;
import org.games.matchmakingservice.domain.MatchResult;
import org.games.matchmakingservice.domain.Player;
import org.games.matchmakingservice.dto.MatchRequestDto;
import org.games.matchmakingservice.dto.MatchResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private EloService eloService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Counter counter;

    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        // Setup Redis template mocks with lenient stubbing
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Setup meter registry mocks with lenient stubbing
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);

        matchmakingService = new MatchmakingService(
            redisTemplate, eloService, messagingTemplate, meterRegistry
        );
    }

    @Test
    void testEnqueuePlayerWithDto_Success() {
        // Given
        MatchRequestDto dto = new MatchRequestDto();
        dto.setPlayerId("testPlayer");
        dto.setElo(1500);

        lenient().when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        lenient().when(zSetOperations.size(anyString())).thenReturn(1L);

        // When
        boolean result = matchmakingService.enqueuePlayer(dto);

        // Then
        assertTrue(result);
        verify(zSetOperations).add(eq("matchmaking:queue"), eq("testPlayer"), anyDouble());
        verify(hashOperations).put(eq("matchmaking:requests"), eq("testPlayer"), anyString());
    }

    @Test
    void testEnqueuePlayerWithDto_NullDto() {
        // When
        boolean result = matchmakingService.enqueuePlayer((MatchRequestDto) null);

        // Then
        assertFalse(result);
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void testDequeuePlayer_NullPlayerId() {
        // When
        boolean result = matchmakingService.dequeuePlayer(null);

        // Then
        assertFalse(result);
        verify(redisTemplate, never()).execute(any(org.springframework.data.redis.core.SessionCallback.class));
    }

    @Test
    void testGetQueueSize() {
        // Given
        when(zSetOperations.size("matchmaking:queue")).thenReturn(5L);

        // When
        long size = matchmakingService.getQueueSize();

        // Then
        assertEquals(5L, size);
        verify(zSetOperations).size("matchmaking:queue");
    }

    @Test
    void testGetQueuePlayers() {
        // Given
        Set<Object> playerIds = new HashSet<>(Arrays.asList("player1", "player2"));
        when(zSetOperations.range("matchmaking:queue", 0, -1)).thenReturn(playerIds);
        when(hashOperations.get("matchmaking:requests", "player1")).thenReturn(
            "{\"playerId\":\"player1\",\"elo\":1500,\"timestamp\":\"2025-08-05T05:00:00Z\"}"
        );
        when(hashOperations.get("matchmaking:requests", "player2")).thenReturn(
            "{\"playerId\":\"player2\",\"elo\":1600,\"timestamp\":\"2025-08-05T05:00:00Z\"}"
        );

        // When
        List<Player> players = matchmakingService.getQueuePlayers();

        // Then
        assertEquals(2, players.size());
        assertEquals("player1", players.get(0).getPlayerId());
        assertEquals("player2", players.get(1).getPlayerId());
    }

    @Test
    void testGetPlayer_Success() {
        // Given
        when(hashOperations.get("matchmaking:requests", "testPlayer")).thenReturn(
            "{\"playerId\":\"testPlayer\",\"elo\":1500,\"timestamp\":\"2025-08-05T05:00:00Z\"}"
        );

        // When
        Player player = matchmakingService.getPlayer("testPlayer");

        // Then
        assertNotNull(player);
        assertEquals("testPlayer", player.getPlayerId());
        assertEquals(1500, player.getElo());
    }

    @Test
    void testGetPlayer_NullPlayerId() {
        // When
        Player player = matchmakingService.getPlayer(null);

        // Then
        assertNull(player);
    }

    @Test
    void testCalculateWinProbability() {
        // Test that higher Elo player has better chance of winning
        double prob1500vs1600 = matchmakingService.calculateWinProbability(1500, 1600);
        double prob1600vs1500 = matchmakingService.calculateWinProbability(1600, 1500);

        assertTrue(prob1500vs1600 < 0.5); // Lower Elo has < 50% chance
        assertTrue(prob1600vs1500 > 0.5); // Higher Elo has > 50% chance
        assertEquals(1.0, prob1500vs1600 + prob1600vs1500, 0.001); // Probabilities should sum to 1
    }

    @Test
    void testConvertToDto() {
        // Given
        MatchResult matchResult = MatchResult.builder()
            .matchId("test-match")
            .playerA("player1")
            .playerB("player2")
            .oldEloA(1500)
            .oldEloB(1600)
            .newEloA(1488)
            .newEloB(1612)
            .winner("player2")
            .playedAt(Instant.now())
            .build();

        // When
        MatchResultDto dto = matchmakingService.convertToDto(matchResult);

        // Then
        assertNotNull(dto);
        assertEquals("test-match", dto.getMatchId());
        assertEquals("player1", dto.getPlayerA());
        assertEquals("player2", dto.getPlayerB());
        assertEquals(1500, dto.getOldEloA());
        assertEquals(1600, dto.getOldEloB());
        assertEquals(1488, dto.getNewEloA());
        assertEquals(1612, dto.getNewEloB());
        assertEquals("player2", dto.getWinner());
    }

    @Test
    void testConvertToDto_NullInput() {
        // When
        MatchResultDto dto = matchmakingService.convertToDto(null);

        // Then
        assertNull(dto);
    }

    @Test
    void testGetRecentMatchResults() {
        // Given
        Map<Object, Object> storedResults = new HashMap<>();
        storedResults.put("match1", "{\"matchId\":\"match1\",\"playerA\":\"player1\",\"playerB\":\"player2\"," +
            "\"oldEloA\":1500,\"oldEloB\":1600,\"newEloA\":1488,\"newEloB\":1612,\"winner\":\"player2\"," +
            "\"playedAt\":\"2025-08-05T05:00:00Z\"}");
        
        when(hashOperations.entries("matchmaking:results")).thenReturn(storedResults);

        // When
        List<MatchResultDto> results = matchmakingService.getRecentMatchResults(10);

        // Then
        assertEquals(1, results.size());
        MatchResultDto result = results.get(0);
        assertEquals("match1", result.getMatchId());
        assertEquals("player1", result.getPlayerA());
        assertEquals("player2", result.getPlayerB());
        assertEquals("player2", result.getWinner());
    }

    @Test
    void testGetRecentMatchResults_EmptyResults() {
        // Given
        when(hashOperations.entries("matchmaking:results")).thenReturn(new HashMap<>());

        // When
        List<MatchResultDto> results = matchmakingService.getRecentMatchResults(10);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void testIsMatchActive() {
        // Given
        when(redisTemplate.hasKey("matchmaking:active:test-match")).thenReturn(true);

        // When
        boolean isActive = matchmakingService.isMatchActive("test-match");

        // Then
        assertTrue(isActive);
        verify(redisTemplate).hasKey("matchmaking:active:test-match");
    }

    @Test
    void testIsMatchActive_NotActive() {
        // Given
        when(redisTemplate.hasKey("matchmaking:active:test-match")).thenReturn(false);

        // When
        boolean isActive = matchmakingService.isMatchActive("test-match");

        // Then
        assertFalse(isActive);
    }

    @Test
    void testCalculateQueueScore() {
        // Given
        Instant pastTime = Instant.now().minusSeconds(10);
        MatchRequest request = MatchRequest.builder()
            .playerId("testPlayer")
            .elo(1500)
            .timestamp(pastTime)
            .build();

        // When
        double score = matchmakingService.calculateQueueScore(request);

        // Then
        // Score should be 1500 (Elo) + wait time bonus
        // Wait time bonus = min(10 seconds, 15 seconds) * 10 = 100
        assertTrue(score >= 1500.0); // Should be at least the Elo rating
        assertTrue(score <= 1650.0); // Should not exceed Elo + max bonus (15 * 10)
        // Allow for timing differences - the score should be between 1500 and 1650
        assertTrue(score >= 1500.0 && score <= 1650.0);
    }

    @Test
    void testGetMatchRequest_Success() {
        // Given
        String jsonData = "{\"playerId\":\"testPlayer\",\"elo\":1500,\"timestamp\":\"2025-08-05T05:00:00Z\"}";
        when(hashOperations.get("matchmaking:requests", "testPlayer")).thenReturn(jsonData);

        // When
        MatchRequest request = matchmakingService.getMatchRequest("testPlayer");

        // Then
        assertNotNull(request);
        assertEquals("testPlayer", request.getPlayerId());
        assertEquals(1500, request.getElo());
    }

    @Test
    void testGetMatchRequest_InvalidJson() {
        // Given
        when(hashOperations.get("matchmaking:requests", "testPlayer")).thenReturn("invalid json");

        // When
        MatchRequest request = matchmakingService.getMatchRequest("testPlayer");

        // Then
        assertNotNull(request);
        assertEquals("testPlayer", request.getPlayerId());
        assertEquals(1500, request.getElo()); // Should use default Elo
    }

    @Test
    void testGetMatchRequest_NullData() {
        // Given
        when(hashOperations.get("matchmaking:requests", "testPlayer")).thenReturn(null);

        // When
        MatchRequest request = matchmakingService.getMatchRequest("testPlayer");

        // Then
        assertNull(request);
    }
} 