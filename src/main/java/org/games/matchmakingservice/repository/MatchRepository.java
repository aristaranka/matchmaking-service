package org.games.matchmakingservice.repository;

import java.util.List;
import org.games.matchmakingservice.domain.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, String> {
    List<MatchEntity> findTop50ByOrderByPlayedAtDesc();
    List<MatchEntity> findByPlayerAOrPlayerBOrderByPlayedAtDesc(String playerA, String playerB);
}


