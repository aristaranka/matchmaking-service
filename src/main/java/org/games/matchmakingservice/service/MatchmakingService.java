package org.games.matchmakingservice.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.games.matchmakingservice.service.WebSocketConnectionTracker;
import org.games.matchmakingservice.domain.MatchRequest;
import org.games.matchmakingservice.domain.MatchResult;
import org.games.matchmakingservice.domain.Player;
import org.games.matchmakingservice.dto.MatchRequestDto;
import org.games.matchmakingservice.dto.MatchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import org.games.matchmakingservice.domain.MatchEntity;
import org.games.matchmakingservice.domain.PlayerStats;
import org.games.matchmakingservice.repository.MatchRepository;
import org.games.matchmakingservice.repository.PlayerStatsRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Core matchmaking service that handles player queuing, matching, and result broadcasting.
 * Uses Redis sorted sets for efficient player matching and scheduled tasks for match processing.
 */
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EloService eloService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;
    private final MatchRepository matchRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final WebSocketConnectionTracker connectionTracker;

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    // Runtime control flag to pause/resume matchmaking loop
    private volatile boolean matchmakingEnabled = true;

    // Redis keys
    private static final String MATCHMAKING_QUEUE = "matchmaking:queue";
    private static final String MATCHMAKING_REQUESTS = "matchmaking:requests"; // Hash for full MatchRequest objects
    private static final String MATCHMAKING_RESULTS = "matchmaking:results";
    private static final String MATCHMAKING_ACTIVE_MATCHES = "matchmaking:active";

    // Configuration
    @Value("${match.max-wait-seconds:15}")
    private int maxWaitTimeSeconds;

    @Value("${match.duration-seconds}")
    private int matchDurationSeconds;

    @Value("${match.elo-tolerance:200}")
    private int eloTolerance; // Maximum Elo difference for matching

    @Value("${match.elo-tolerance-growth-per-second:5}")
    private double toleranceGrowthPerSecond; // How much to widen tolerance per second waited

    @Value("${match.elo-tolerance-max:800}")
    private int maxEloTolerance; // Cap on widened tolerance

    /**
     * Enqueue a player into the matchmaking queue.
     * 
     * @param matchRequest The player's match request
     * @return true if successfully enqueued, false otherwise
     */
    public boolean enqueuePlayer(MatchRequest matchRequest) {
        if (matchRequest == null) {
            log.warn("Cannot enqueue null match request");
            return false;
        }
        
        try {
            String playerId = matchRequest.getPlayerId();
            double score = calculateQueueScore(matchRequest);
            
            log.debug("Attempting to enqueue player {} with score {}", playerId, score);
            
            // Add to sorted set with score (Elo + wait time bonus) for ordering
            redisTemplate.opsForZSet().add(MATCHMAKING_QUEUE, playerId, score);
            
            log.debug("Successfully added player {} to ZSET", playerId);
            
            // Store essential data in hash for metadata preservation
            String jsonData = String.format("{\"playerId\":\"%s\",\"elo\":%d,\"timestamp\":\"%s\"}", 
                playerId, matchRequest.getElo(), matchRequest.getTimestamp());
            
            log.debug("Storing JSON data for player {}: {}", playerId, jsonData);
            
            redisTemplate.opsForHash().put(MATCHMAKING_REQUESTS, playerId, jsonData);
            
            log.debug("Successfully stored MatchRequest for player {}", playerId);
            
            // Set TTL for stale requests (30 minutes)
            redisTemplate.expire(MATCHMAKING_QUEUE, Duration.ofMinutes(30));
            redisTemplate.expire(MATCHMAKING_REQUESTS, Duration.ofMinutes(30));
            
            // Update metrics
            meterRegistry.gauge("matchmaking.queue.size", 
                redisTemplate.opsForZSet().size(MATCHMAKING_QUEUE));
            
            log.info("Player {} enqueued with Elo {} and score {}",
                    playerId, matchRequest.getElo(), score);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to enqueue player {}: {} - Exception type: {}", 
                matchRequest.getPlayerId(), e.getMessage(), e.getClass().getSimpleName(), e);
            return false;
        }
    }

    /**
     * Enqueue a player into the matchmaking queue using DTO.
     * 
     * @param matchRequestDto The player's match request DTO
     * @return true if successfully enqueued, false otherwise
     */
    public boolean enqueuePlayer(MatchRequestDto matchRequestDto) {
        if (matchRequestDto == null) {
            log.warn("Cannot enqueue null match request DTO");
            return false;
        }
        
        // Convert DTO to domain object
        MatchRequest matchRequest = MatchRequest.builder()
            .playerId(matchRequestDto.getPlayerId())
            .elo(matchRequestDto.getElo())
            .timestamp(Instant.now())
            .build();
        
        return enqueuePlayer(matchRequest);
    }

    /**
     * Remove a player from the matchmaking queue using Redis transaction.
     * 
     * @param playerId The player ID to remove
     * @return true if successfully removed, false otherwise
     */
    public boolean dequeuePlayer(String playerId) {
        if (playerId == null) {
            log.warn("Cannot dequeue player with null playerId");
            return false;
        }
        
        try {
            // Use Redis transaction for atomic removal
            Boolean result = redisTemplate.execute(new SessionCallback<>() {
                @Override
                public @NonNull Boolean execute(org.springframework.data.redis.core.RedisOperations operations) throws org.springframework.data.redis.RedisSystemException {
                    operations.multi();

                    // Remove from both ZSET and hash atomically
                    operations.opsForZSet().remove(MATCHMAKING_QUEUE, playerId);
                    operations.opsForHash().delete(MATCHMAKING_REQUESTS, playerId);

                    List<Object> results = operations.exec();

                    // Check if removal was successful
                    Long removedFromQueue = (Long) results.getFirst();
                    return removedFromQueue != null && removedFromQueue > 0;
                }
            });
            
            if (Boolean.TRUE.equals(result)) {
                log.info("Player {} removed from matchmaking queue", playerId);
                
                // Update metrics
                meterRegistry.gauge("matchmaking.queue.size", 
                    redisTemplate.opsForZSet().size(MATCHMAKING_QUEUE));
                
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to dequeue player {}", playerId, e);
            return false;
        }
    }

    /**
     * Process matchmaking automatically on a schedule.
     * Finds the best matches and creates games.
     */
    @Scheduled(fixedRateString = "${app.match.poll-rate-ms:1000}")
    public void processMatchmaking() {
        try {
            if (!matchmakingEnabled) {
                log.debug("Matchmaking is paused; skipping this cycle");
                return;
            }
            
            // Check if there are any active WebSocket connections
            if (!connectionTracker.hasActiveConnections()) {
                log.debug("No active WebSocket connections; skipping matchmaking to preserve queue for observation");
                return;
            }
            // Process matches until no more pairs can be made
            while (true) {
                // Get all players in queue sorted by Elo (ascending)
                Set<ZSetOperations.TypedTuple<Object>> playersWithScores = 
                    redisTemplate.opsForZSet().rangeWithScores(MATCHMAKING_QUEUE, 0, -1);
                
                if (playersWithScores.size() < 2) {
                    break; // Not enough players for a match
                }
                
                // Convert to list and prioritize by wait duration, then Elo
                List<PlayerWithRequest> players = playersWithScores.stream()
                    .map(tuple -> {
                        String playerId = tuple.getValue().toString();
                        MatchRequest request = getMatchRequest(playerId);
                        return new PlayerWithRequest(playerId, request);
                    })
                    .filter(player -> player.request != null) // Filter out any missing requests
                    .sorted(Comparator
                        .<PlayerWithRequest>comparingLong(p -> getWaitSeconds(p.request)).reversed()
                        .thenComparingInt(p -> p.request.getElo()))
                    .toList();
                
                // Find the best match
                MatchPair bestMatch = findBestMatch(players);
                
                if (bestMatch == null) {
                    break; // No compatible matches found
                }
                
                // Get the MatchRequest data BEFORE removing players from Redis
                MatchRequest requestA = getMatchRequest(bestMatch.playerA);
                MatchRequest requestB = getMatchRequest(bestMatch.playerB);
                
                if (requestA == null || requestB == null) {
                    log.error("Missing match request data for players: {} or {}", bestMatch.playerA, bestMatch.playerB);
                    break;
                }
                
                // Use Redis transaction for atomic removal
                Boolean matchCreated = redisTemplate.execute(new SessionCallback<>() {
                    @Override
                    public Boolean execute(org.springframework.data.redis.core.RedisOperations operations) throws org.springframework.data.redis.RedisSystemException {
                        operations.multi();
                        
                        // Remove both players from queue and hash atomically
                        operations.opsForZSet().remove(MATCHMAKING_QUEUE, bestMatch.playerA, bestMatch.playerB);
                        operations.opsForHash().delete(MATCHMAKING_REQUESTS, bestMatch.playerA, bestMatch.playerB);
                        
                        List<Object> results = operations.exec();
                        
                        // Check if removal was successful
                        Long removedFromQueue = (Long) results.getFirst();
                        return removedFromQueue != null && removedFromQueue == 2;
                    }
                });
                
                if (Boolean.TRUE.equals(matchCreated)) {
                    // Create match with the data we captured before removal
                    createMatchWithData(bestMatch.playerA, bestMatch.playerB, requestA, requestB);
                    log.info("Matched players: {} (Elo: {}) vs {} (Elo: {})", 
                            bestMatch.playerA, bestMatch.eloA, bestMatch.playerB, bestMatch.eloB);
                    
                    // Update metrics
                    meterRegistry.counter("matchmaking.matches.made").increment();
                    meterRegistry.gauge("matchmaking.queue.size", 
                        redisTemplate.opsForZSet().size(MATCHMAKING_QUEUE));
                } else {
                    log.warn("Failed to remove matched players from queue: {} and {}", 
                            bestMatch.playerA, bestMatch.playerB);
                }
            }
            
        } catch (Exception e) {
            log.error("Match loop failed", e);
        }
    }

    /**
     * Pause the automatic matchmaking loop.
     */
    public void pauseMatchmaking() {
        matchmakingEnabled = false;
        log.info("Matchmaking has been paused");
    }

    /**
     * Resume the automatic matchmaking loop.
     */
    public void resumeMatchmaking() {
        matchmakingEnabled = true;
        log.info("Matchmaking has been resumed");
    }

    /**
     * Check if matchmaking loop is enabled.
     */
    public boolean isMatchmakingEnabled() {
        return matchmakingEnabled;
    }

    /**
     * Get the full MatchRequest object for a player.
     * 
     * @param playerId The player ID
     * @return The MatchRequest object, or null if not found
     */
    MatchRequest getMatchRequest(String playerId) {
        try {
            Object stored = redisTemplate.opsForHash().get(MATCHMAKING_REQUESTS, playerId);
            log.debug("Retrieved stored data for player {}: {}", playerId, stored);
            
            if (stored instanceof String) {
                // Parse the JSON string to extract Elo and timestamp
                String jsonString = (String) stored;
                log.debug("Parsing JSON string: {}", jsonString);
                
                // Extract Elo
                Integer elo = null;
                if (jsonString.contains("\"elo\":")) {
                    int eloStart = jsonString.indexOf("\"elo\":") + 6;
                    int eloEnd = jsonString.indexOf(",", eloStart);
                    if (eloEnd == -1) {
                        eloEnd = jsonString.indexOf("}", eloStart);
                    }
                    if (eloEnd > eloStart) {
                        elo = Integer.parseInt(jsonString.substring(eloStart, eloEnd));
                    }
                }
                // Extract timestamp
                Instant ts = Instant.now();
                try {
                    String tsKey = "\"timestamp\":\"";
                    int tsStart = jsonString.indexOf(tsKey);
                    if (tsStart >= 0) {
                        tsStart += tsKey.length();
                        int tsEnd = jsonString.indexOf("\"", tsStart);
                        if (tsEnd > tsStart) {
                            String tsStr = jsonString.substring(tsStart, tsEnd);
                            ts = Instant.parse(tsStr);
                        }
                    }
                } catch (Exception ignored) {}
                
                if (elo != null) {
                    log.debug("Extracted Elo {} and timestamp {} for player {}", elo, ts, playerId);
                    return MatchRequest.builder()
                        .playerId(playerId)
                        .elo(elo)
                        .timestamp(ts)
                        .build();
                }
                
                log.warn("Could not parse Elo from JSON string: {}", jsonString);
                // Fallback to default Elo
                return MatchRequest.builder()
                    .playerId(playerId)
                    .elo(1500) // Default Elo
                    .timestamp(Instant.now())
                    .build();
            }
            log.debug("Stored data is not a string: {}", stored != null ? stored.getClass().getSimpleName() : "null");
            return (MatchRequest) stored;
        } catch (Exception e) {
            log.error("Failed to get match request for player {}", playerId, e);
            return null;
        }
    }

    /**
     * Find the best match from a sorted list of players.
     * 
     * @param players List of players sorted by Elo (ascending)
     * @return Best match pair, or null if no compatible match found
     */
    private MatchPair findBestMatch(List<PlayerWithRequest> players) {
        if (players.size() < 2) {
            return null;
        }
        
        // For each player, find the partner within dynamic tolerance, prioritizing longer waits
        for (int i = 0; i < players.size() - 1; i++) {
            PlayerWithRequest playerA = players.get(i);
            
            // Find compatible partner within dynamic tolerance
            PlayerWithRequest bestPartner = null;
            int bestEloDifference = Integer.MAX_VALUE;
            long bestMinWait = -1;
            
            for (int j = i + 1; j < players.size(); j++) {
                PlayerWithRequest playerB = players.get(j);
                int eloA = playerA.request.getElo();
                int eloB = playerB.request.getElo();
                int eloDifference = Math.abs(eloA - eloB);
                
                int tolA = computeDynamicTolerance(playerA.request);
                int tolB = computeDynamicTolerance(playerB.request);
                int allowedDifference = Math.min(tolA, tolB);
                
                log.debug("Checking match: {} (Elo: {}) vs {} (Elo: {}), diff: {}, allowed: {} (tolA: {}, tolB: {})", 
                    playerA.playerId, eloA, playerB.playerId, eloB, eloDifference, allowedDifference, tolA, tolB);
                
                if (eloDifference <= allowedDifference) {
                    long waitA = getWaitSeconds(playerA.request);
                    long waitB = getWaitSeconds(playerB.request);
                    long minWait = Math.min(waitA, waitB);
                    if (bestPartner == null || minWait > bestMinWait || (minWait == bestMinWait && eloDifference < bestEloDifference)) {
                        bestPartner = playerB;
                        bestEloDifference = eloDifference;
                        bestMinWait = minWait;
                        log.debug("Found compatible match: {} vs {} with diff {} (minWait {}s)", 
                            playerA.playerId, playerB.playerId, eloDifference, minWait);
                    }
                }
            }
            
            // If we found a compatible partner, return the match
            if (bestPartner != null) {
                return new MatchPair(playerA.playerId, bestPartner.playerId, 
                                   playerA.request.getElo(), bestPartner.request.getElo());
            }
        }
        
        return null; // No compatible matches found
    }

    private long getWaitSeconds(MatchRequest request) {
        try {
            return Math.max(0L, Duration.between(request.getTimestamp(), Instant.now()).getSeconds());
        } catch (Exception e) {
            return 0L;
        }
    }

    private int computeDynamicTolerance(MatchRequest request) {
        long waitSeconds = getWaitSeconds(request);
        long widened = Math.round(toleranceGrowthPerSecond * waitSeconds);
        long candidate = (long) eloTolerance + widened;
        return (int) Math.min(candidate, maxEloTolerance);
    }

    /**
     * Create a match between two players with pre-captured data.
     * 
     * @param playerA Player A ID
     * @param playerB Player B ID
     * @param requestA Player A's match request data
     * @param requestB Player B's match request data
     */
    private void createMatchWithData(String playerA, String playerB, MatchRequest requestA, MatchRequest requestB) {
        try {
            log.debug("Creating match between {} and {} with pre-captured data", playerA, playerB);
            String matchId = UUID.randomUUID().toString();
            
            int oldEloA = requestA.getElo();
            int oldEloB = requestB.getElo();
            
            // Determine winner with Elo-based probability
            // Higher Elo player has better chance of winning
            double playerAWinProbability = calculateWinProbability(oldEloA, oldEloB);
            boolean playerAWins = Math.random() < playerAWinProbability;
            
            // Calculate new ratings using EloService
            EloService.EloResult eloResult = playerAWins ? 
                eloService.calculateWinForPlayerA(oldEloA, oldEloB) :
                eloService.calculateWinForPlayerB(oldEloA, oldEloB);
            
            // Create match result with all required fields
            MatchResult matchResult = MatchResult.builder()
                    .matchId(matchId)
                    .playerA(playerA)
                    .playerB(playerB)
                    .oldEloA(oldEloA)
                    .oldEloB(oldEloB)
                    .newEloA(eloResult.ratingA())
                    .newEloB(eloResult.ratingB())
                    .winner(playerAWins ? playerA : playerB)
                    .playedAt(Instant.now())
                    .build();
            
            // Store match result
            storeMatchResult(matchResult);
            
            // Broadcast result
            broadcastMatchResult(matchResult);

            // Persist to database and update player stats
            persistMatchAndStats(matchResult);
            
            // Schedule match end
            scheduleMatchEnd(matchId);
            
            log.info("Match completed: {} (Elo: {}) vs {} (Elo: {}). Winner: {} (New Elo: {} vs {})", 
                    playerA, oldEloA, playerB, oldEloB, matchResult.getWinner(),
                    matchResult.getNewEloA(), matchResult.getNewEloB());
                    
        } catch (Exception e) {
            log.error("Failed to create match between {} and {}", playerA, playerB, e);
        }
    }

    private void persistMatchAndStats(MatchResult matchResult) {
        try {
            // Save match row
            MatchEntity entity = new MatchEntity();
            entity.setMatchId(matchResult.getMatchId());
            entity.setPlayerA(matchResult.getPlayerA());
            entity.setPlayerB(matchResult.getPlayerB());
            entity.setOldEloA(matchResult.getOldEloA());
            entity.setOldEloB(matchResult.getOldEloB());
            entity.setNewEloA(matchResult.getNewEloA());
            entity.setNewEloB(matchResult.getNewEloB());
            entity.setWinner(matchResult.getWinner());
            entity.setPlayedAt(matchResult.getPlayedAt());
            matchRepository.save(entity);

            // Update stats for both players
            boolean aWon = matchResult.getWinner().equals(matchResult.getPlayerA());
            updatePlayerStatsForResult(matchResult.getPlayerA(), matchResult.getNewEloA(), aWon);
            updatePlayerStatsForResult(matchResult.getPlayerB(), matchResult.getNewEloB(), !aWon);
        } catch (Exception e) {
            log.error("Failed to persist match and stats {}", matchResult.getMatchId(), e);
        }
    }

    private void updatePlayerStatsForResult(String playerId, int newElo, boolean won) {
        PlayerStats stats = playerStatsRepository.findById(playerId).orElseGet(() -> {
            PlayerStats s = new PlayerStats();
            s.setPlayerId(playerId);
            s.setUsername(playerId);
            s.setCurrentElo(newElo);
            s.setWins(0L);
            s.setLosses(0L);
            s.setGames(0L);
            return s;
        });
        stats.setCurrentElo(newElo);
        stats.setGames(stats.getGames() + 1);
        if (won) {
            stats.setWins(stats.getWins() + 1);
        } else {
            stats.setLosses(stats.getLosses() + 1);
        }
        playerStatsRepository.save(stats);
    }

    /**
     * Calculate queue score based on Elo and wait time.
     * 
     * @param matchRequest The match request
     * @return Composite score for queue positioning
     */
    double calculateQueueScore(MatchRequest matchRequest) {
        long waitTimeSeconds = Instant.now().getEpochSecond() - 
                             matchRequest.getTimestamp().getEpochSecond();
        
        // Base score is Elo rating
        double score = matchRequest.getElo();
        
        // Add wait time bonus (max 30 seconds)
        double waitTimeBonus = Math.min(waitTimeSeconds, maxWaitTimeSeconds) * 10;
        
        return score + waitTimeBonus;
    }

    /**
     * Get player Elo rating (simplified implementation).
     * In real implementation, this would fetch from player database.
     * 
     * @param playerId Player ID
     * @return Player's Elo rating
     */
    private int getPlayerElo(String playerId) {
        // Get the full MatchRequest to access original Elo
        MatchRequest request = getMatchRequest(playerId);
        return request != null ? request.getElo() : 1500; // Default Elo
    }

    /**
     * Get Player object for a player ID.
     * This method can be used to get additional player information beyond what's in MatchRequest.
     * 
     * @param playerId The player ID
     * @return Player object, or null if not found
     */
    public Player getPlayer(String playerId) {
        if (playerId == null) {
            log.warn("Cannot get player with null playerId");
            return null;
        }
        
        try {
            // In a real implementation, this would fetch from a player database
            // For now, we'll create a Player object from the MatchRequest data
            MatchRequest request = getMatchRequest(playerId);
            if (request != null) {
                return Player.builder()
                    .playerId(playerId)
                    .username(playerId) // In real implementation, this would be the actual username
                    .elo(request.getElo())
                    .lastActive(Instant.now())
                    .online(true)
                    .build();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get player {}", playerId, e);
            return null;
        }
    }

    /**
     * Get queue statistics with detailed player information.
     * 
     * @return List of players currently in queue
     */
    public List<Player> getQueuePlayers() {
        try {
            Set<Object> playerIds = redisTemplate.opsForZSet().range(MATCHMAKING_QUEUE, 0, -1);
            
            return playerIds.stream()
                .filter(Objects::nonNull)
                .map(id -> getPlayer(id.toString()))
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception e) {
            log.error("Failed to get queue players", e);
            return List.of();
        }
    }

    /**
     * Store match result in Redis.
     * 
     * @param matchResult The match result to store
     */
    private void storeMatchResult(MatchResult matchResult) {
        try {
            log.debug("Attempting to store match result: {}", matchResult.getMatchId());
            
            // Store as JSON string to avoid serialization issues
            String jsonResult = String.format(
                "{\"matchId\":\"%s\",\"playerA\":\"%s\",\"playerB\":\"%s\",\"oldEloA\":%d,\"oldEloB\":%d,\"newEloA\":%d,\"newEloB\":%d,\"winner\":\"%s\",\"playedAt\":\"%s\"}",
                matchResult.getMatchId(),
                matchResult.getPlayerA(),
                matchResult.getPlayerB(),
                matchResult.getOldEloA(),
                matchResult.getOldEloB(),
                matchResult.getNewEloA(),
                matchResult.getNewEloB(),
                matchResult.getWinner(),
                matchResult.getPlayedAt()
            );
            
            log.debug("JSON result: {}", jsonResult);
            
            redisTemplate.opsForHash().put(MATCHMAKING_RESULTS, 
                    matchResult.getMatchId(), jsonResult);
            
            // Set expiration for cleanup (e.g., 24 hours)
            redisTemplate.expire(MATCHMAKING_RESULTS, 24, TimeUnit.HOURS);
            
            log.info("Successfully stored match result: {}", matchResult.getMatchId());
            
        } catch (Exception e) {
            log.error("Failed to store match result {}", matchResult.getMatchId(), e);
        }
    }

    /**
     * Broadcast match result via WebSocket.
     * 
     * @param matchResult The match result to broadcast
     */
    private void broadcastMatchResult(MatchResult matchResult) {
        try {
            log.info("Broadcasting match result via WebSocket: matchId={}, playerA={}, playerB={}, winner={}", 
                    matchResult.getMatchId(), matchResult.getPlayerA(), matchResult.getPlayerB(), matchResult.getWinner());
            messagingTemplate.convertAndSend("/topic/matches", matchResult);
            log.info("Successfully broadcasted match result: {}", matchResult.getMatchId());
        } catch (Exception e) {
            log.error("Failed to broadcast match result {}", matchResult.getMatchId(), e);
        }
    }

    /**
     * Schedule match end after specified duration.
     * 
     * @param matchId The match ID
     */
    private void scheduleMatchEnd(String matchId) {
        try {
            // Store active match with expiration
            redisTemplate.opsForValue().set(
                MATCHMAKING_ACTIVE_MATCHES + ":" + matchId, 
                "active", 
                matchDurationSeconds,
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Failed to schedule match end for {}", matchId, e);
        }
    }

    /**
     * Check if a match is still active.
     * 
     * @param matchId The match ID
     * @return true if match is active, false otherwise
     */
    public boolean isMatchActive(String matchId) {
        try {
            return redisTemplate.hasKey(MATCHMAKING_ACTIVE_MATCHES + ":" + matchId);
        } catch (Exception e) {
            log.error("Failed to check match status for {}", matchId, e);
            return false;
        }
    }

    /**
     * Calculate win probability based on Elo difference.
     * Uses the same formula as Elo rating system.
     * 
     * @param eloA Player A's Elo
     * @param eloB Player B's Elo
     * @return Probability that Player A wins (0.0 to 1.0)
     */
    double calculateWinProbability(int eloA, int eloB) {
        double eloDifference = eloA - eloB;
        double expectedScore = 1.0 / (1.0 + Math.pow(10, -eloDifference / 400.0));
        return expectedScore;
    }

    /**
     * Get queue statistics.
     * 
     * @return Number of players in queue
     */
    public long getQueueSize() {
        try {
            return redisTemplate.opsForZSet().size(MATCHMAKING_QUEUE);
        } catch (Exception e) {
            log.error("Failed to get queue size", e);
            return 0;
        }
    }

    /**
     * Parse JSON string to MatchResultDto.
     * 
     * @param jsonString The JSON string to parse
     * @return MatchResultDto or null if parsing fails
     */
    private MatchResultDto parseMatchResultFromJson(String jsonString) {
        try {
            // Simple JSON parsing to extract fields
            String matchId = extractJsonField(jsonString, "matchId");
            String playerA = extractJsonField(jsonString, "playerA");
            String playerB = extractJsonField(jsonString, "playerB");
            String winner = extractJsonField(jsonString, "winner");
            
            Integer oldEloA = extractJsonIntField(jsonString, "oldEloA");
            Integer oldEloB = extractJsonIntField(jsonString, "oldEloB");
            Integer newEloA = extractJsonIntField(jsonString, "newEloA");
            Integer newEloB = extractJsonIntField(jsonString, "newEloB");
            
            String playedAtStr = extractJsonField(jsonString, "playedAt");
            Instant playedAt = playedAtStr != null ? Instant.parse(playedAtStr) : Instant.now();
            
            // Use builder pattern to avoid constructor parameter order issues
            return MatchResultDto.builder()
                .matchId(matchId)
                .playerA(playerA)
                .playerB(playerB)
                .oldEloA(oldEloA)
                .oldEloB(oldEloB)
                .newEloA(newEloA)
                .newEloB(newEloB)
                .winner(winner)
                .playedAt(playedAt)
                .build();
        } catch (Exception e) {
            log.error("Failed to parse match result JSON: {}", jsonString, e);
            return null;
        }
    }
    
    /**
     * Extract string field from JSON.
     */
    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    /**
     * Extract integer field from JSON.
     */
    private Integer extractJsonIntField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /**
     * Convert MatchResult to MatchResultDto for API responses.
     * 
     * @param matchResult The domain MatchResult
     * @return MatchResultDto for API response
     */
    public MatchResultDto convertToDto(MatchResult matchResult) {
        if (matchResult == null) {
            log.warn("Cannot convert null MatchResult to DTO");
            return null;
        }
        
        return MatchResultDto.builder()
            .matchId(matchResult.getMatchId())
            .playerA(matchResult.getPlayerA())
            .playerB(matchResult.getPlayerB())
            .oldEloA(matchResult.getOldEloA())
            .oldEloB(matchResult.getOldEloB())
            .newEloA(matchResult.getNewEloA())
            .newEloB(matchResult.getNewEloB())
            .winner(matchResult.getWinner())
            .playedAt(matchResult.getPlayedAt())
            .build();
    }

    /**
     * Get recent match results as DTOs.
     * 
     * @param limit Maximum number of results to return
     * @return List of recent match results as DTOs
     */
    public List<MatchResultDto> getRecentMatchResults(int limit) {
        try {
            log.debug("Getting recent match results (limit: {})", limit);
            
            // Get all match results from Redis hash
            Map<Object, Object> allResults = redisTemplate.opsForHash().entries(MATCHMAKING_RESULTS);
            log.debug("Found {} results in Redis", allResults.size());
            
            List<MatchResultDto> results = new ArrayList<>();
            
            for (Map.Entry<Object, Object> entry : allResults.entrySet()) {
                try {
                    Object value = entry.getValue();
                    log.debug("Processing result: {}", value.getClass().getSimpleName());
                    
                    if (value instanceof String) {
                        // Parse JSON string to MatchResultDto
                        String jsonString = (String) value;
                        MatchResultDto dto = parseMatchResultFromJson(jsonString);
                        if (dto != null) {
                            results.add(dto);
                        }
                    } else if (value instanceof MatchResult) {
                        MatchResultDto dto = convertToDto((MatchResult) value);
                        if (dto != null) {
                            results.add(dto);
                        }
                    } else {
                        log.warn("Unexpected result type: {}", value.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.error("Error processing match result: {}", entry.getKey(), e);
                }
            }
            
            // Sort by most recent first
            results.sort((a, b) -> b.getPlayedAt().compareTo(a.getPlayedAt()));
            
            // Limit results
            if (results.size() > limit) {
                results = results.subList(0, limit);
            }
            
            log.debug("Returning {} match results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to get recent match results", e);
            return List.of();
        }
    }

    /**
     * Get recent matches for a player or overall history from DB.
     */
    public List<MatchResultDto> getMatchHistory(String playerId, int limit) {
        try {
            List<MatchEntity> entities;
            if (playerId == null || playerId.isBlank()) {
                entities = matchRepository.findTop50ByOrderByPlayedAtDesc();
            } else {
                entities = matchRepository.findByPlayerAOrPlayerBOrderByPlayedAtDesc(playerId, playerId);
            }
            return entities.stream()
                .limit(limit)
                .map(this::convertEntityToDto)
                .toList();
        } catch (Exception e) {
            log.error("Failed to get match history for {}", playerId, e);
            return List.of();
        }
    }

    private MatchResultDto convertEntityToDto(MatchEntity entity) {
        return MatchResultDto.builder()
            .matchId(entity.getMatchId())
            .playerA(entity.getPlayerA())
            .playerB(entity.getPlayerB())
            .oldEloA(entity.getOldEloA())
            .oldEloB(entity.getOldEloB())
            .newEloA(entity.getNewEloA())
            .newEloB(entity.getNewEloB())
            .winner(entity.getWinner())
            .playedAt(entity.getPlayedAt())
            .build();
    }

    /**
     * Get a basic leaderboard ordered by current Elo.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        try {
            List<PlayerStats> top = playerStatsRepository.findTop100ByOrderByCurrentEloDesc();
            if (limit > 0 && top.size() > limit) {
                top = top.subList(0, limit);
            }
            int[] rank = {1};
            return top.stream().map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("rank", rank[0]++);
                m.put("playerId", s.getPlayerId());
                m.put("username", s.getUsername());
                m.put("elo", s.getCurrentElo());
                m.put("wins", s.getWins());
                m.put("losses", s.getLosses());
                m.put("games", s.getGames());
                return m;
            }).toList();
        } catch (Exception e) {
            log.error("Failed to get leaderboard", e);
            return List.of();
        }
    }

    /**
     * Helper class for players with their full MatchRequest objects.
     */
    private static class PlayerWithRequest {
        String playerId;
        MatchRequest request;

        PlayerWithRequest(String playerId, MatchRequest request) {
            this.playerId = playerId;
            this.request = request;
        }
    }

    /**
     * Helper class for match pairs.
     */
    private static class MatchPair {
        String playerA;
        String playerB;
        int eloA;
        int eloB;

        MatchPair(String playerA, String playerB, int eloA, int eloB) {
            this.playerA = playerA;
            this.playerB = playerB;
            this.eloA = eloA;
            this.eloB = eloB;
        }
    }
} 