package org.games.matchmakingservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Elo Service Tests")
@SpringBootTest
@TestPropertySource(properties = {
    "elo.kfactor.default=32",
    "elo.scale.factor=400.0",
    "elo.kfactor.expert.threshold=2100",
    "elo.kfactor.master.threshold=2400"
})
class EloServiceTest {

    @Autowired
    private EloService eloService;

    @Test
    @DisplayName("Should calculate expected score correctly")
    void shouldCalculateExpectedScore() {
        // Equal ratings should result in 0.5 expected score
        assertEquals(0.5, eloService.calculateExpectedScore(1500, 1500), 0.01);
        
        // Higher rated player should have higher expected score
        assertTrue(eloService.calculateExpectedScore(1600, 1400) > 0.5);
        assertTrue(eloService.calculateExpectedScore(1400, 1600) < 0.5);
    }

    @Test
    @DisplayName("Should calculate win for player A correctly")
    void shouldCalculateWinForPlayerA() {
        EloService.EloResult result = eloService.calculateWinForPlayerA(1500, 1500);
        
        // Player A should gain points, Player B should lose points
        assertTrue(result.ratingA() > 1500);
        assertTrue(result.ratingB() < 1500);
        
        // Total rating should remain the same (zero-sum game)
        assertEquals(3000, result.ratingA() + result.ratingB());
    }

    @Test
    @DisplayName("Should calculate win for player B correctly")
    void shouldCalculateWinForPlayerB() {
        EloService.EloResult result = eloService.calculateWinForPlayerB(1500, 1500);
        
        // Player B should gain points, Player A should lose points
        assertTrue(result.ratingB() > 1500);
        assertTrue(result.ratingA() < 1500);
        
        // Total rating should remain the same (zero-sum game)
        assertEquals(3000, result.ratingA() + result.ratingB());
    }

    @Test
    @DisplayName("Should calculate draw correctly")
    void shouldCalculateDraw() {
        EloService.EloResult result = eloService.calculateDraw(1500, 1500);
        
        // In a draw between equal players, ratings should stay the same
        assertEquals(1500, result.ratingA());
        assertEquals(1500, result.ratingB());
    }

    @Test
    @DisplayName("Should handle rating differences correctly")
    void shouldHandleRatingDifferences() {
        // Test with moderate rating difference
        EloService.EloResult result = eloService.calculateWinForPlayerA(1600, 1400);
        
        // Verify that ratings change appropriately
        assertTrue(result.ratingA() > 1600); // Player A gains points
        assertTrue(result.ratingB() < 1400); // Player B loses points
        assertEquals(3000, result.ratingA() + result.ratingB()); // Total rating remains constant
        
        // Test upset scenario
        EloService.EloResult upsetResult = eloService.calculateWinForPlayerB(1600, 1400);
        
        // Verify upset results
        assertTrue(upsetResult.ratingA() < 1600); // Player A loses points
        assertTrue(upsetResult.ratingB() > 1400); // Player B gains points
        assertEquals(3000, upsetResult.ratingA() + upsetResult.ratingB()); // Total rating remains constant
    }

    @Test
    @DisplayName("Should return correct K-factor based on rating")
    void shouldReturnCorrectKFactor() {
        assertEquals(32, eloService.getKFactor(1500)); // Standard
        assertEquals(24, eloService.getKFactor(2100)); // Expert
        assertEquals(16, eloService.getKFactor(2400)); // Master
    }

    @Test
    @DisplayName("Should throw exception for negative ratings")
    void shouldThrowExceptionForNegativeRatings() {
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.calculateExpectedScore(-100, 1500));
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.calculateExpectedScore(1500, -100));
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.calculateNewRatings(-100, 1500, 1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.calculateNewRatings(1500, -100, 1.0, 0.0));
    }

    @Test
    @DisplayName("Should throw exception for invalid scores")
    void shouldThrowExceptionForInvalidScores() {
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.calculateNewRatings(1500, 1500, -0.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.calculateNewRatings(1500, 1500, 1.5, 0.0));
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.calculateNewRatings(1500, 1500, 0.5, 1.5));
    }

    @Test
    @DisplayName("Should handle valid score ranges")
    void shouldHandleValidScoreRanges() {
        // These should not throw exceptions
        assertDoesNotThrow(() -> 
            eloService.calculateNewRatings(1500, 1500, 0.0, 1.0));
        assertDoesNotThrow(() -> 
            eloService.calculateNewRatings(1500, 1500, 0.5, 0.5));
        assertDoesNotThrow(() -> 
            eloService.calculateNewRatings(1500, 1500, 1.0, 0.0));
    }

    @Test
    @DisplayName("Should handle invalid k-factors")
    void shouldHandleInvalidKFactors() {
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.getKFactor(-100));
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.getKFactor(-3000));
        assertThrows(IllegalArgumentException.class, () -> 
            eloService.getKFactor(-500));
    }
} 