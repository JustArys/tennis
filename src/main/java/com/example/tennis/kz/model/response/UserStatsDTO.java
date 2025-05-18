package com.example.tennis.kz.model.response;

import com.example.tennis.kz.model.TournamentTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {
    private long totalMatchesPlayed;
    private long totalWins;
    private long totalLosses;

    private Map<TournamentTier, Long> winsByTier; // Wins per tournament tier

    private long singlesMatchesPlayed;
    private long singlesWins;
    private long doublesMatchesPlayed;
    private long doublesWins;

    // Пример вычисляемого поля (если нужно сериализовать, можно добавить @JsonProperty)
    // Или этот метод будет использоваться только на бэкенде/во фронтенде после получения DTO
    public double getOverallWinRatio() {
        if (totalMatchesPlayed == 0) {
            return 0.0;
        }
        return (double) totalWins / totalMatchesPlayed;
    }

    public double getSinglesWinRatio() {
        if (singlesMatchesPlayed == 0) {
            return 0.0;
        }
        return (double) singlesWins / singlesMatchesPlayed;
    }

    public double getDoublesWinRatio() {
        if (doublesMatchesPlayed == 0) {
            return 0.0;
        }
        return (double) doublesWins / doublesMatchesPlayed;
    }
}