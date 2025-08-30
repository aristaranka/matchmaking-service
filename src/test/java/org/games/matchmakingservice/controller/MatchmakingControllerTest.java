package org.games.matchmakingservice.controller;

import org.games.matchmakingservice.domain.Player;
import org.games.matchmakingservice.dto.MatchRequestDto;
import org.games.matchmakingservice.dto.MatchResultDto;
import org.games.matchmakingservice.service.MatchmakingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingControllerTest {

    @Mock
    private MatchmakingService matchmakingService;

    private MatchmakingController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchmakingController(matchmakingService);
    }

    @Test
    void testJoinMatchmaking_Success() {
        // Given
        MatchRequestDto request = new MatchRequestDto();
        request.setPlayerId("testPlayer");
        request.setElo(1500);

        when(matchmakingService.enqueuePlayer(any(MatchRequestDto.class))).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.joinMatchmaking(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("Successfully joined matchmaking queue", body.get("message"));
        assertEquals("testPlayer", body.get("playerId"));
        assertEquals(1500, body.get("elo"));
    }

    @Test
    void testJoinMatchmaking_Failure() {
        // Given
        MatchRequestDto request = new MatchRequestDto();
        request.setPlayerId("testPlayer");
        request.setElo(1500);

        when(matchmakingService.enqueuePlayer(any(MatchRequestDto.class))).thenReturn(false);

        // When
        ResponseEntity<Map<String, Object>> response = controller.joinMatchmaking(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Failed to join matchmaking queue", body.get("message"));
    }

    @Test
    void testJoinMatchmaking_Exception() {
        // Given
        MatchRequestDto request = new MatchRequestDto();
        request.setPlayerId("testPlayer");
        request.setElo(1500);

        when(matchmakingService.enqueuePlayer(any(MatchRequestDto.class))).thenThrow(new RuntimeException("Test exception"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.joinMatchmaking(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Internal server error", body.get("message"));
    }

    @Test
    void testLeaveMatchmaking_Success() {
        // Given
        String playerId = "testPlayer";
        when(matchmakingService.dequeuePlayer(playerId)).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = controller.leaveMatchmaking(playerId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("Successfully left matchmaking queue", body.get("message"));
        assertEquals(playerId, body.get("playerId"));
    }

    @Test
    void testLeaveMatchmaking_PlayerNotFound() {
        // Given
        String playerId = "testPlayer";
        when(matchmakingService.dequeuePlayer(playerId)).thenReturn(false);

        // When
        ResponseEntity<Map<String, Object>> response = controller.leaveMatchmaking(playerId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Player not found in queue", body.get("message"));
    }

    @Test
    void testLeaveMatchmaking_Exception() {
        // Given
        String playerId = "testPlayer";
        when(matchmakingService.dequeuePlayer(playerId)).thenThrow(new RuntimeException("Test exception"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.leaveMatchmaking(playerId);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Internal server error", body.get("message"));
    }

    @Test
    void testGetMatchmakingStatus_Success() {
        // Given
        when(matchmakingService.getQueueSize()).thenReturn(2L);
        
        Player player1 = new Player();
        player1.setPlayerId("player1");
        player1.setUsername("player1");
        player1.setElo(1500);
        player1.setOnline(true);
        player1.setLastActive(Instant.now());

        Player player2 = new Player();
        player2.setPlayerId("player2");
        player2.setUsername("player2");
        player2.setElo(1600);
        player2.setOnline(true);
        player2.setLastActive(Instant.now());

        when(matchmakingService.getQueuePlayers()).thenReturn(List.of(player1, player2));

        // When
        ResponseEntity<Map<String, Object>> response = controller.getMatchmakingStatus();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2L, body.get("queueSize"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queuePlayers = (List<Map<String, Object>>) body.get("queuePlayers");
        assertNotNull(queuePlayers);
        assertEquals(2, queuePlayers.size());
        assertEquals("player1", queuePlayers.get(0).get("playerId"));
        assertEquals("player2", queuePlayers.get(1).get("playerId"));
    }

    @Test
    void testGetMatchmakingStatus_Exception() {
        // Given
        when(matchmakingService.getQueueSize()).thenThrow(new RuntimeException("Test exception"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.getMatchmakingStatus();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Internal server error", body.get("message"));
    }

    @Test
    void testGetPlayerInfo_Success() {
        // Given
        String playerId = "testPlayer";
        Player player = new Player();
        player.setPlayerId(playerId);
        player.setUsername("testPlayer");
        player.setElo(1500);
        player.setOnline(true);
        player.setLastActive(Instant.now());

        when(matchmakingService.getPlayer(playerId)).thenReturn(player);

        // When
        ResponseEntity<Map<String, Object>> response = controller.getPlayerInfo(playerId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> playerInfo = (Map<String, Object>) body.get("player");
        assertNotNull(playerInfo);
        assertEquals(playerId, playerInfo.get("playerId"));
        assertEquals(1500, playerInfo.get("elo"));
    }

    @Test
    void testGetPlayerInfo_PlayerNotFound() {
        // Given
        String playerId = "testPlayer";
        when(matchmakingService.getPlayer(playerId)).thenReturn(null);

        // When
        ResponseEntity<Map<String, Object>> response = controller.getPlayerInfo(playerId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Player not found", body.get("message"));
    }

    @Test
    void testGetPlayerInfo_Exception() {
        // Given
        String playerId = "testPlayer";
        when(matchmakingService.getPlayer(playerId)).thenThrow(new RuntimeException("Test exception"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.getPlayerInfo(playerId);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Internal server error", body.get("message"));
    }

    @Test
    void testGetMatchResults_Success() {
        // Given
        MatchResultDto result1 = new MatchResultDto();
        result1.setMatchId("match1");
        result1.setPlayerA("player1");
        result1.setPlayerB("player2");
        result1.setOldEloA(1500);
        result1.setOldEloB(1600);
        result1.setNewEloA(1488);
        result1.setNewEloB(1612);
        result1.setWinner("player2");
        result1.setPlayedAt(Instant.now());

        when(matchmakingService.getRecentMatchResults(10)).thenReturn(List.of(result1));

        // When
        ResponseEntity<Map<String, Object>> response = controller.getMatchResults(10);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(1, body.get("count"));
        
        @SuppressWarnings("unchecked")
        List<MatchResultDto> results = (List<MatchResultDto>) body.get("results");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("match1", results.get(0).getMatchId());
    }

    @Test
    void testGetMatchResults_Exception() {
        // Given
        when(matchmakingService.getRecentMatchResults(10)).thenThrow(new RuntimeException("Test exception"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.getMatchResults(10);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Internal server error", body.get("message"));
    }

    @Test
    void testHealth() {
        // When
        ResponseEntity<Map<String, Object>> response = controller.health();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertEquals("matchmaking", body.get("service"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testGetHistory_Success() {
        MatchResultDto dto = MatchResultDto.builder()
            .matchId("m1")
            .playerA("alice")
            .playerB("bob")
            .oldEloA(1200)
            .oldEloB(1210)
            .newEloA(1216)
            .newEloB(1194)
            .winner("alice")
            .playedAt(Instant.now())
            .build();

        when(matchmakingService.getMatchHistory("alice", 10)).thenReturn(List.of(dto));

        ResponseEntity<Map<String, Object>> response = controller.getHistory("alice", 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        @SuppressWarnings("unchecked")
        List<MatchResultDto> results = (List<MatchResultDto>) body.get("results");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("m1", results.get(0).getMatchId());
    }

    @Test
    void testGetHistory_Exception() {
        when(matchmakingService.getMatchHistory(null, 20)).thenThrow(new RuntimeException("Test"));

        ResponseEntity<Map<String, Object>> response = controller.getHistory(null, 20);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
    }

    @Test
    void testLeaderboard_Success() {
        Map<String, Object> row1 = Map.of(
            "rank", 1,
            "playerId", "bob",
            "username", "bob",
            "elo", 1300,
            "wins", 10L,
            "losses", 2L,
            "games", 12L
        );
        Map<String, Object> row2 = Map.of(
            "rank", 2,
            "playerId", "alice",
            "username", "alice",
            "elo", 1280,
            "wins", 8L,
            "losses", 4L,
            "games", 12L
        );

        when(matchmakingService.getLeaderboard(5)).thenReturn(List.of(row1, row2));

        ResponseEntity<Map<String, Object>> response = controller.leaderboard(5);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> leaders = (List<Map<String, Object>>) body.get("leaders");
        assertNotNull(leaders);
        assertEquals(2, leaders.size());
        assertEquals(1, leaders.get(0).get("rank"));
        assertEquals("bob", leaders.get(0).get("playerId"));
    }

    @Test
    void testLeaderboard_Exception() {
        when(matchmakingService.getLeaderboard(10)).thenThrow(new RuntimeException("Test"));

        ResponseEntity<Map<String, Object>> response = controller.leaderboard(10);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
    }

    @Test
    void testPauseEndpoint() {
        // When
        ResponseEntity<Map<String, Object>> response = controller.pauseMatchmaking();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(false, body.get("enabled"));
        verify(matchmakingService).pauseMatchmaking();
    }

    @Test
    void testResumeEndpoint() {
        // When
        ResponseEntity<Map<String, Object>> response = controller.resumeMatchmaking();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(true, body.get("enabled"));
        verify(matchmakingService).resumeMatchmaking();
    }

    @Test
    void testEnabledEndpoint() {
        when(matchmakingService.isMatchmakingEnabled()).thenReturn(true);
        ResponseEntity<Map<String, Object>> response = controller.getEnabled();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(true, body.get("enabled"));
    }
} 