package org.games.matchmakingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_stats")
public class PlayerStats {

    @Id
    @Column(name = "player_id", nullable = false, length = 128)
    private String playerId;

    @Column(name = "username", length = 128)
    private String username;

    @Column(name = "current_elo", nullable = false)
    private Integer currentElo;

    @Column(name = "wins", nullable = false)
    private Long wins;

    @Column(name = "losses", nullable = false)
    private Long losses;

    @Column(name = "games", nullable = false)
    private Long games;

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getCurrentElo() { return currentElo; }
    public void setCurrentElo(Integer currentElo) { this.currentElo = currentElo; }
    public Long getWins() { return wins; }
    public void setWins(Long wins) { this.wins = wins; }
    public Long getLosses() { return losses; }
    public void setLosses(Long losses) { this.losses = losses; }
    public Long getGames() { return games; }
    public void setGames(Long games) { this.games = games; }
}


