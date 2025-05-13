package com.example.tennis.kz.model;

import lombok.Getter;

@Getter
public enum TournamentTier {
    FUTURES(200, 16, 4),  // Общие очки, Макс. участников, Кол-во сеяных
    CHALLENGER(400, 32, 8),
    MASTERS(600, 64, 16);

    private final int totalPoints;
    private final int maxParticipants;
    private final int numberOfSeeds;

    TournamentTier(int totalPoints, int maxParticipants, int numberOfSeeds) {
        this.totalPoints = totalPoints;
        this.maxParticipants = maxParticipants;
        this.numberOfSeeds = numberOfSeeds;
    }

    public int getPointsForRound(int roundReached, int totalRoundsInTournament) {
        if (totalRoundsInTournament <= 0) return 0;

        // Победитель (достиг раунда > чем общее кол-во раундов)
        if (roundReached > totalRoundsInTournament) return totalPoints;
        // Финалист (достиг финального раунда)
        if (roundReached == totalRoundsInTournament) return (int) Math.round(totalPoints * 0.60); // 60%
        // Полуфиналист
        if (roundReached == totalRoundsInTournament - 1) return (int) Math.round(totalPoints * 0.36); // 36% (Пример WТА: 1000 -> 650 -> 390)
        if (roundReached == totalRoundsInTournament - 2 && totalRoundsInTournament >= 3) return (int) Math.round(totalPoints * 0.19); // 19%
        // 1/8 финала (R16 для Masters/Challenger)
        if (roundReached == totalRoundsInTournament - 3 && totalRoundsInTournament >= 4) return (int) Math.round(totalPoints * 0.10); // 10%
        // 1/16 финала (R32 для Masters)
        if (roundReached == totalRoundsInTournament - 4 && totalRoundsInTournament >= 5) return (int) Math.round(totalPoints * 0.055); // 5.5%
        // 1/32 финала (R64 для Masters) - первый раунд для 64 участников
        if (roundReached == totalRoundsInTournament - 5 && totalRoundsInTournament >= 6) return (int) Math.round(totalPoints * 0.01); // 1% (или 10 очков)

        // (т.е. roundReached == 1 И totalRoundsInTournament > 1)
        // Можно дать минимальные очки за участие, если это предусмотрено (например, для Futures)
        if (roundReached == 1 && totalRoundsInTournament > 1 && this == FUTURES) return (int) Math.round(totalPoints * 0.02); // Например, 5 очков для Futures (2.5% от 200)
        if (roundReached == 1 && totalRoundsInTournament > 1 ) return 0; // для остальных категорий

        return 0;
    }

}