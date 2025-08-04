package org.games.matchmakingservice.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for calculating Elo rating changes based on match outcomes.
 * Implements the standard Elo rating system with configurable K-factor.
 */
@Slf4j
@Service
public class EloService {

    @Value("${elo.kfactor.default:32}")
    private int defaultKFactor;

    @Value("${elo.scale.factor:400.0}")
    private double ratingDifferenceScale;

    @Value("${elo.kfactor.expert.threshold:2100}")
    private int expertThreshold;

    @Value("${elo.kfactor.master.threshold:2400}")
    private int masterThreshold;

    /**
     * Calculate the expected score for a player against an opponent.
     * 
     * @param playerRating The player's current Elo rating
     * @param opponentRating The opponent's Elo rating
     * @return Expected score (0.0 to 1.0)
     */
    public double calculateExpectedScore(int playerRating, int opponentRating) {
        // Validate ratings
        if (playerRating < 0 || opponentRating < 0) {
                throw new IllegalArgumentException("Ratings must be non-negative");
            }
            double ratingDifference = opponentRating - playerRating;
            double exponent = ratingDifference / ratingDifferenceScale;
            return 1.0 / (1.0 + Math.pow(10, exponent));
    }

    /**
     * Calculates the new Elo ratings for both players after a match using the default K-factor.
     *
     * @param playerARating Player A's current rating
     * @param playerBRating Player B's current rating
     * @param playerAScore Player A's actual score (1.0 for win, 0.5 for draw, 0.0 for loss)
     * @param playerBScore Player B's actual score (1.0 for win, 0.5 for draw, 0.0 for loss)
     * @return EloResult containing the new ratings for Player A and Player B
     */
    public EloResult calculateNewRatings(int playerARating, int playerBRating,
                                         double playerAScore, double playerBScore) {
        int kFactorA = getKFactor(playerARating);
        int kFactorB = getKFactor(playerBRating);
        return calculateNewRatings(playerARating, playerBRating, playerAScore, playerBScore, kFactorA, kFactorB);
    }

    /**
     * Calculates the new Elo ratings for both players after a match with custom K-factor.
     * 
     * @param playerARating Player A's current rating
     * @param playerBRating Player B's current rating
     * @param playerAScore Player A's actual score (1.0 for win, 0.5 for draw, 0.0 for loss)
     * @param playerBScore Player B's actual score (1.0 for win, 0.5 for draw, 0.0 for loss)
     * @param kFactor The K-factor for rating adjustments
     * @return EloResult containing the new ratings for Player A and Player B
     */
    public EloResult calculateNewRatings(int playerARating, int playerBRating,
                                         double playerAScore, double playerBScore,
                                         int kFactorA, int kFactorB) {
        // Validate inputs
        if (playerARating < 0 || playerBRating < 0) {
            throw new IllegalArgumentException("Ratings must be non-negative");
        }
        if (playerAScore < 0.0 || playerAScore > 1.0 ||
            playerBScore < 0.0 || playerBScore > 1.0) {
            throw new IllegalArgumentException("Scores must be between 0.0 and 1.0");
        }
        
        // Calculate expected scores
        double expectedScoreA = calculateExpectedScore(playerARating, playerBRating);
        double expectedScoreB = calculateExpectedScore(playerBRating, playerARating);
        
        // Calculate rating changes
        int ratingChangeA = (int) Math.round(kFactorA * (playerAScore - expectedScoreA));
        int ratingChangeB = (int) Math.round(kFactorB * (playerBScore - expectedScoreB));
        
        // Calculate new ratings
        int newRatingA = playerARating + ratingChangeA;
        int newRatingB = playerBRating + ratingChangeB;
        
        log.debug("Elo calculation: A({} -> {}) B({} -> {}) K={} K={}",
                  playerARating, newRatingA, playerBRating, newRatingB, kFactorA, kFactorB);

        return new EloResult(newRatingA, newRatingB);
    }

    /**
     * Calculate new ratings for a win by player A.
     * 
     * @param playerARating Player A's current rating
     * @param playerBRating Player B's current rating
     * @return EloResult containing the new ratings for Player A and Player B
     */
    public EloResult calculateWinForPlayerA(int playerARating, int playerBRating) {
                return calculateNewRatings(playerARating, playerBRating, 1.0, 0.0);
            }

    /**
     * Calculate new ratings for a win by player B.
     * 
     * @param playerARating Player A's current rating
     * @param playerBRating Player B's current rating
     * @return EloResult containing the new ratings for Player A and Player B
     */
    public EloResult calculateWinForPlayerB(int playerARating, int playerBRating) {
        return calculateNewRatings(playerARating, playerBRating, 0.0, 1.0);
    }

    /**
     * Calculate new ratings for a draw.
     * 
     * @param playerARating Player A's current rating
     * @param playerBRating Player B's current rating
     * @return EloResult containing the new ratings for Player A and Player B
     */
    public EloResult calculateDraw(int playerARating, int playerBRating) {
        return calculateNewRatings(playerARating, playerBRating, 0.5, 0.5);
    }

    /**
     * Get the K-factor based on player rating (higher ratings = lower K-factor).
     * 
     * @param playerRating The player's current rating
     * @return The appropriate K-factor
     */
    public int getKFactor(int playerRating) {
        if (playerRating < 0) {
            throw new IllegalArgumentException("Ratings must be non-negative");
        }
        if (playerRating >= masterThreshold) {
            return 16; // Master level
        } else if (playerRating >= expertThreshold) {
            return 24; // Expert level
        } else {
            return defaultKFactor; // Standard level
        }
    }

    /**
     * Simple holder for updated Elo ratings.
     */
    public static record EloResult(int ratingA, int ratingB) {}
} 