package org.games.matchmakingservice.repository;

import java.util.List;
import org.games.matchmakingservice.domain.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, String> {
    List<PlayerStats> findTop100ByOrderByCurrentEloDesc();
}


