package org.games.matchmakingservice.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 + * Immutable request for entering matchmaking.
 + */
@Value
@Builder
public class MatchRequest {
    @NonNull
    String playerId;

    @NonNull
    Integer elo;

    /** when request was created */
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant timestamp = Instant.now();
    }
