package org.games.matchmakingservice.domain;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 *Immutable result of a matchmaking event.
 */
@Value
@Builder
public class MatchResult {
    @NonNull
    String matchId;

    @NonNull
    String playerA;

    @NonNull
    String playerB;

    @NonNull
    Integer newEloA;

    @NonNull
    Integer oldEloA;

    @NonNull
    Integer oldEloB;

    @NonNull
    Integer newEloB;

    @NonNull
    String winner;

    /** when the match was played */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant playedAt;
    }
