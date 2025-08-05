package org.games.matchmakingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.games.matchmakingservice.domain.Player;
import org.games.matchmakingservice.dto.MatchRequestDto;
import org.games.matchmakingservice.dto.MatchResultDto;
import org.games.matchmakingservice.service.MatchmakingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for matchmaking operations.
 * Provides endpoints for joining/leaving matchmaking queue and checking status.
 */
@Slf4j
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    /**
     * Join the matchmaking queue.
     * 
     * @param request The match request containing player ID and Elo
     * @return Response indicating success or failure
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinMatchmaking(@Valid @RequestBody MatchRequestDto request) {
        try {
            log.info("Player {} joining matchmaking queue with Elo {}", 
                    request.getPlayerId(), request.getElo());
            
            boolean success = matchmakingService.enqueuePlayer(request);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully joined matchmaking queue",
                    "playerId", request.getPlayerId(),
                    "elo", request.getElo()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", "Failed to join matchmaking queue",
                        "playerId", request.getPlayerId()
                    ));
            }
        } catch (Exception e) {
            log.error("Error joining matchmaking queue for player {}", request.getPlayerId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Internal server error",
                    "playerId", request.getPlayerId()
                ));
        }
    }

    /**
     * Leave the matchmaking queue.
     * 
     * @param playerId The player ID to remove from queue
     * @return Response indicating success or failure
     */
    @DeleteMapping("/leave/{playerId}")
    public ResponseEntity<Map<String, Object>> leaveMatchmaking(@PathVariable String playerId) {
        try {
            log.info("Player {} leaving matchmaking queue", playerId);
            
            boolean success = matchmakingService.dequeuePlayer(playerId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully left matchmaking queue",
                    "playerId", playerId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", "Player not found in queue",
                        "playerId", playerId
                    ));
            }
        } catch (Exception e) {
            log.error("Error leaving matchmaking queue for player {}", playerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Internal server error",
                    "playerId", playerId
                ));
        }
    }

    /**
     * Get matchmaking status and queue information.
     * 
     * @return Current queue status and statistics
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMatchmakingStatus() {
        try {
            long queueSize = matchmakingService.getQueueSize();
            List<Player> queuePlayers = matchmakingService.getQueuePlayers();
            
            Map<String, Object> status = Map.of(
                "queueSize", queueSize,
                "queuePlayers", queuePlayers.stream()
                    .map(player -> Map.of(
                        "playerId", player.getPlayerId(),
                        "username", player.getUsername(),
                        "elo", player.getElo(),
                        "online", player.getOnline(),
                        "lastActive", player.getLastActive()
                    ))
                    .toList(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting matchmaking status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Internal server error"
                ));
        }
    }

    /**
     * Get player information.
     * 
     * @param playerId The player ID
     * @return Player information
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<Map<String, Object>> getPlayerInfo(@PathVariable String playerId) {
        try {
            Player player = matchmakingService.getPlayer(playerId);
            
            if (player != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "player", Map.of(
                        "playerId", player.getPlayerId(),
                        "username", player.getUsername(),
                        "elo", player.getElo(),
                        "online", player.getOnline(),
                        "lastActive", player.getLastActive()
                    )
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", "Player not found",
                        "playerId", playerId
                    ));
            }
        } catch (Exception e) {
            log.error("Error getting player info for {}", playerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Internal server error",
                    "playerId", playerId
                ));
        }
    }

    /**
     * Get recent match results.
     * 
     * @param limit Maximum number of results to return (default: 10)
     * @return Recent match results
     */
    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getMatchResults(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<MatchResultDto> results = matchmakingService.getRecentMatchResults(limit);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", results,
                "count", results.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Error getting match results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Internal server error"
                ));
        }
    }

    /**
     * Health check endpoint.
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "matchmaking",
            "timestamp", System.currentTimeMillis()
        ));
    }
} 