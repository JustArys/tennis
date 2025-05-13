package com.example.tennis.kz.service; // или com.example.tennis.kz.service.impl

import com.example.tennis.kz.model.*;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import com.example.tennis.kz.repository.UserInfoRepository; // Убедись, что этот репозиторий существует
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsService { // Класс вместо интерфейса + реализации

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final UserInfoRepository userInfoRepository;

    @Transactional
    public void calculateAndAwardPointsForTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found with ID: " + tournamentId));

        Match finalMatch = tournament.getMatches().stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == tournament.getTotalRounds())
                .findFirst()
                .orElse(null);

        if (finalMatch == null || (finalMatch.getStatus() != MatchStatus.COMPLETED && finalMatch.getStatus() != MatchStatus.WALKOVER)) {
            throw new IllegalStateException("Tournament is not yet finished. Final match not yet completed. Cannot calculate points.");
        }

        List<TournamentRegistration> registrations = tournamentRegistrationRepository.findByTournamentId(tournamentId);
        int totalRoundsInTournament = tournament.getTotalRounds();

        for (TournamentRegistration reg : registrations) {
            int roundReached = 0; // Раунд, до которого игрок дошел (т.е. в котором он проиграл или который выиграл, если это финал)

            // Если участник - победитель финального матча
            if (finalMatch.getWinner() != null && finalMatch.getWinner().equals(reg)) {
                roundReached = totalRoundsInTournament + 1; // Специальное значение для победителя турнира
            } else {
                // Ищем максимальный раунд, в котором участник играл
                int maxRoundPlayedThisParticipant = 0;
                boolean lostInKnownMatch = false;

                for (Match match : tournament.getMatches()) {
                    boolean participatedInThisMatch = (match.getParticipant1() != null && match.getParticipant1().equals(reg)) ||
                            (match.getParticipant2() != null && match.getParticipant2().equals(reg));

                    if (participatedInThisMatch) {
                        maxRoundPlayedThisParticipant = Math.max(maxRoundPlayedThisParticipant, match.getRoundNumber());

                        // Если это матч, где он проиграл (победитель есть и это не он)
                        if (match.getWinner() != null && !match.getWinner().equals(reg) &&
                                (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER)) {
                            roundReached = match.getRoundNumber(); // Дошел до этого раунда и проиграл
                            lostInKnownMatch = true;
                            break;
                        }
                    }
                }
                // Если не нашли явного матча проигрыша, но он играл в финале (и не выиграл его, т.к. это проверено выше)
                if (!lostInKnownMatch && maxRoundPlayedThisParticipant == totalRoundsInTournament) {
                    roundReached = totalRoundsInTournament; // Значит, финалист (проиграл в финале)
                } else if (!lostInKnownMatch && maxRoundPlayedThisParticipant > 0) {
                    // Если он играл, но не проиграл ни в одном матче (например, все его последующие соперники снялись)
                    // Это сложный случай. Для простоты, считаем, что он дошел до максимального раунда, где участвовал.
                    // Либо, если его последний матч еще не COMPLETED/WALKOVER, но турнир завершен - это ошибка в данных.
                    roundReached = maxRoundPlayedThisParticipant;
                }
            }

            if (roundReached == 0 && registrations.size() > 1 && tournament.getTotalRounds() > 0) {
                // System.out.println("Warning: Participant " + reg.getId() + " in tournament " + tournamentId +
                //                  " seems to have not played or their exit round is unclear. Awarding 0 points by default for this participant.");
            }


            int pointsAwarded = tournament.getTier().getPointsForRound(roundReached, totalRoundsInTournament);
            // System.out.println("Tournament " + tournamentId + ": RegID " + reg.getId() + ", RoundReached: " + roundReached + ", Points: " + pointsAwarded);

            if (pointsAwarded > 0) {
                awardPointsToPlayer(reg.getUser(), pointsAwarded);
                if (tournament.getTournamentType() == TournamentType.DOUBLES && reg.getPartner() != null) {
                    awardPointsToPlayer(reg.getPartner(), pointsAwarded);
                }
            }
        }
        // System.out.println("Points calculation completed for tournament ID: " + tournamentId);
    }

    private void awardPointsToPlayer(User player, int points) {
        if (player == null || player.getUserInfo() == null) {
            // System.err.println("Warning: Attempted to award points to a null player or player with no UserInfo.");
            return;
        }
        UserInfo userInfo = player.getUserInfo();
        Float currentRating = userInfo.getRating() != null ? userInfo.getRating() : 0f;
        userInfo.setRating(currentRating + points);
        userInfoRepository.save(userInfo); // Убедись, что userInfoRepository инжектирован
        // System.out.println("Awarded " + points + " points to player ID: " + player.getId() + ". New rating: " + userInfo.getRating());
    }
}