package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Импорт
import com.example.tennis.kz.model.*;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import com.example.tennis.kz.repository.UserInfoRepository;
// import jakarta.persistence.EntityNotFoundException; // Заменяем
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException; // Импорт

@Service
@RequiredArgsConstructor
public class PointsService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final UserInfoRepository userInfoRepository;

    @Transactional
    public void calculateAndAwardPointsForTournament(Long tournamentId) {
        if (tournamentId == null) {
            throw new BadRequestException("ID турнира не может быть null.");
        }
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new NoSuchElementException("Турнир с ID: " + tournamentId + " не найден."));

        Match finalMatch = tournament.getMatches().stream()
                .filter(m -> m.getRoundNumber() != null && m.getRoundNumber() == tournament.getTotalRounds())
                .findFirst()
                .orElse(null); // Оставляем orElse(null) для явной проверки ниже

        if (finalMatch == null || (finalMatch.getStatus() != MatchStatus.COMPLETED && finalMatch.getStatus() != MatchStatus.WALKOVER)) {
            // Если клиент пытается вычислить очки для незавершенного турнира, это некорректный запрос.
            throw new BadRequestException("Турнир еще не завершен или финальный матч не отмечен как COMPLETED/WALKOVER. Невозможно рассчитать очки.");
        }

        List<TournamentRegistration> registrations = tournamentRegistrationRepository.findByTournamentId(tournamentId);
        if (registrations.isEmpty() && tournament.getTotalRounds() > 0) {

            System.out.println("Нет регистраций для расчета очков в турнире ID: " + tournamentId);
            return;
        }

        int totalRoundsInTournament = tournament.getTotalRounds();

        for (TournamentRegistration reg : registrations) {
            int roundReached = 0;

            if (finalMatch.getWinner() != null && finalMatch.getWinner().equals(reg)) {
                roundReached = totalRoundsInTournament + 1;
            } else {
                int maxRoundPlayedThisParticipant = 0;
                boolean lostInKnownMatch = false;

                for (Match match : tournament.getMatches()) {
                    boolean participatedInThisMatch = (match.getParticipant1() != null && match.getParticipant1().equals(reg)) ||
                            (match.getParticipant2() != null && match.getParticipant2().equals(reg));

                    if (participatedInThisMatch) {
                        if (match.getRoundNumber() == null) {
                            // Это указывает на проблему с данными матча
                            throw new IllegalStateException("Нарушение целостности данных: номер раунда не установлен для матча ID " + match.getId() + " в турнире " + tournamentId);
                        }
                        maxRoundPlayedThisParticipant = Math.max(maxRoundPlayedThisParticipant, match.getRoundNumber());

                        if (match.getWinner() != null && !match.getWinner().equals(reg) &&
                                (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER)) {
                            roundReached = match.getRoundNumber();
                            lostInKnownMatch = true;
                            break;
                        }
                    }
                }
                if (!lostInKnownMatch && maxRoundPlayedThisParticipant == totalRoundsInTournament) {
                    roundReached = totalRoundsInTournament;
                } else if (!lostInKnownMatch && maxRoundPlayedThisParticipant > 0) {
                    roundReached = maxRoundPlayedThisParticipant;
                }
            }

            // Логика для случая, когда roundReached == 0 остается как предупреждение.
            // Если это критическая ошибка, можно было бы выбросить IllegalStateException.

            int pointsAwarded = tournament.getTier().getPointsForRound(roundReached, totalRoundsInTournament);

            if (pointsAwarded > 0) {
                awardPointsToPlayer(reg.getUser(), pointsAwarded);
                if (tournament.getTournamentType() == TournamentType.DOUBLES && reg.getPartner() != null) {
                    awardPointsToPlayer(reg.getPartner(), pointsAwarded);
                }
            }
        }
    }

    private void awardPointsToPlayer(User player, int points) {
        if (points <= 0) return; // Не начисляем 0 или отрицательные очки

        if (player == null) {
            // Это неожиданная ситуация, если логика выше предполагает наличие игрока
            throw new IllegalStateException("Попытка начислить очки null игроку.");
        }
        if (player.getUserInfo() == null) {
            // Также неожиданно, если UserInfo должно всегда существовать для игрока
            throw new IllegalStateException("Попытка начислить очки игроку (ID: " + player.getId() + ") без UserInfo.");
        }

        UserInfo userInfo = player.getUserInfo();
        Float currentPoints = userInfo.getPoints() != null ? userInfo.getPoints() : 0f;
        userInfo.setPoints(currentPoints + points);
        userInfoRepository.save(userInfo);
    }
}