package org.games.matchmakingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_matches_played_at", columnList = "played_at DESC"),
    @Index(name = "idx_matches_player_a", columnList = "player_a"),
    @Index(name = "idx_matches_player_b", columnList = "player_b")
})
public class MatchEntity {

    @Id
    @Column(name = "match_id", nullable = false, length = 64)
    private String matchId;

    @Column(name = "player_a", nullable = false, length = 128)
    private String playerA;

    @Column(name = "player_b", nullable = false, length = 128)
    private String playerB;

    @Column(name = "old_elo_a", nullable = false)
    private Integer oldEloA;

    @Column(name = "old_elo_b", nullable = false)
    private Integer oldEloB;

    @Column(name = "new_elo_a", nullable = false)
    private Integer newEloA;

    @Column(name = "new_elo_b", nullable = false)
    private Integer newEloB;

    @Column(name = "winner", nullable = false, length = 128)
    private String winner;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getPlayerA() { return playerA; }
    public void setPlayerA(String playerA) { this.playerA = playerA; }
    public String getPlayerB() { return playerB; }
    public void setPlayerB(String playerB) { this.playerB = playerB; }
    public Integer getOldEloA() { return oldEloA; }
    public void setOldEloA(Integer oldEloA) { this.oldEloA = oldEloA; }
    public Integer getOldEloB() { return oldEloB; }
    public void setOldEloB(Integer oldEloB) { this.oldEloB = oldEloB; }
    public Integer getNewEloA() { return newEloA; }
    public void setNewEloA(Integer newEloA) { this.newEloA = newEloA; }
    public Integer getNewEloB() { return newEloB; }
    public void setNewEloB(Integer newEloB) { this.newEloB = newEloB; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public Instant getPlayedAt() { return playedAt; }
    public void setPlayedAt(Instant playedAt) { this.playedAt = playedAt; }
}


