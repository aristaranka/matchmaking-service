package org.games.matchmakingservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDto {
    private String matchId;
    private String playerA;
    private String playerB;
    private Integer oldEloA;
    private Integer oldEloB;
    private Integer newEloA;
    private Integer newEloB;
    private String winner;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant playedAt;
} 