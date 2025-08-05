package org.games.matchmakingservice.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable request for entering matchmaking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequest {
    @JsonProperty("playerId")
    private String playerId;

    @JsonProperty("elo")
    private Integer elo;

    /** when request was created */
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @JsonProperty("timestamp")
    private Instant timestamp = Instant.now();
}
