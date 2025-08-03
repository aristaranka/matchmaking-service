package org.games.matchmakingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;


public class MatchRequestDto {

    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotNull(message = "Elo is required")
    @Min(value = 0, message = "Elo must be >= 0")
    private Integer elo;
} 