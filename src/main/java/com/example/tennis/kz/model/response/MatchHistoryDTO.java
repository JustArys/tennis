package com.example.tennis.kz.model.response;

import com.example.tennis.kz.model.Category;
import com.example.tennis.kz.model.TournamentTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchHistoryDTO {
    private Long matchId;
    private String tournamentName;
    private TournamentTier tournamentTier;
    private Category tournamentCategory; // e.g., SINGLES_MALE, DOUBLE_MIXED
    private String roundName; // e.g., Final, Semi-final, Round 1
    private LocalDateTime matchDate; // completedTime or scheduledTime if not completed
    private String participant1Name;
    private String participant2Name;
    private String score;
    private String winnerName; // Name of the winning participant/pair
    private Boolean currentUserWon; // True if the user (for whom history is fetched) won this match
    private String opponentName; // Name of the opponent (or opponents if pair)
}