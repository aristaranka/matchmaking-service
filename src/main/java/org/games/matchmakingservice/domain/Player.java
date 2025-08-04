package org.games.matchmakingservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String playerId;
    private String username;
    private Integer elo;
    private Instant lastActive;
    private Boolean online;
} 