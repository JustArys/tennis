
package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Наш кастомный BadRequestException
import com.example.tennis.kz.model.*;
import com.example.tennis.kz.model.response.MatchHistoryDTO;
import com.example.tennis.kz.model.response.UserStatsDTO;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.UserRepository;
// import jakarta.persistence.EntityNotFoundException; // Будет заменен на NoSuchElementException
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays; // Импортируем Arrays для EnumMap initialization
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException; // Стандартный NoSuchElementException

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final UserService userService; // Для получения текущего аутентифицированного пользователя

    @Transactional(readOnly = true)
    public Page<MatchHistoryDTO> getCurrentUserMatchHistory(Pageable pageable) {
        if (pageable == null) {
            throw new BadRequestException("Параметр Pageable не может быть null.");
        }
        User currentUser = userService.getAuthenticatedUser();
        if (currentUser == null || currentUser.getId() == null) {
            // Эта ситуация должна быть обработана Spring Security (401/403)
            // или userService.getAuthenticatedUser() должен выбрасывать исключение.
            // Для защиты, если userService может вернуть null или неполного пользователя:
            throw new IllegalStateException("Не удалось получить данные аутентифицированного пользователя.");
        }
        Page<Match> matchesPage = matchRepository.findMatchesByUserId(currentUser.getId(), pageable);
        return matchesPage.map(match -> convertToMatchHistoryDTO(match, currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public Page<MatchHistoryDTO> getUserMatchHistory(Long userId, Pageable pageable) {
        if (userId == null) {
            throw new BadRequestException("ID пользователя не может быть null.");
        }
        if (pageable == null) {
            throw new BadRequestException("Параметр Pageable не может быть null.");
        }
        // Проверка, существует ли пользователь
        User user = userRepository.findById(userId) // Сначала получаем пользователя
                .orElseThrow(() -> new NoSuchElementException("Пользователь с ID: " + userId + " не найден."));
        // Если пользователь найден, user.getId() будет корректным
        Page<Match> matchesPage = matchRepository.findMatchesByUserId(user.getId(), pageable);
        return matchesPage.map(match -> convertToMatchHistoryDTO(match, user.getId()));
    }

    private MatchHistoryDTO convertToMatchHistoryDTO(Match match, Long perspectiveUserId) {
        // perspectiveUserId здесь предполагается non-null после проверок в публичных методах
        if (match == null) {
            throw new IllegalStateException("Попытка конвертировать null матч в DTO. Это не ожидается.");
        }
        MatchHistoryDTO dto = new MatchHistoryDTO();
        dto.setMatchId(match.getId());

        Tournament tournament = match.getTournament();
        if (tournament != null) {
            dto.setTournamentName(tournament.getDescription());
            dto.setTournamentTier(tournament.getTier());
            dto.setTournamentCategory(tournament.getCategory());
        } else {
            // Если матч не привязан к турниру, это может быть ошибкой данных
            // throw new IllegalStateException("Матч ID " + match.getId() + " не имеет связанного турнира.");
            // Или просто оставляем поля пустыми/N/A, как было:
            dto.setTournamentName("N/A");
        }

        dto.setRoundName(match.getRoundName());
        dto.setMatchDate(match.getCompletedTime() != null ? match.getCompletedTime() : match.getScheduledTime());

        TournamentRegistration p1Reg = match.getParticipant1();
        TournamentRegistration p2Reg = match.getParticipant2();

        String p1Name = (p1Reg != null) ? p1Reg.getParticipantName() : "N/A";
        String p2Name = (p2Reg != null) ? p2Reg.getParticipantName() : "N/A";
        dto.setParticipant1Name(p1Name);
        dto.setParticipant2Name(p2Name);
        dto.setScore(match.getScore());

        boolean currentUserIsP1 = p1Reg != null &&
                ((p1Reg.getUser() != null && p1Reg.getUser().getId().equals(perspectiveUserId)) ||
                        (p1Reg.getPartner() != null && p1Reg.getPartner().getId().equals(perspectiveUserId)));
        boolean currentUserIsP2 = p2Reg != null &&
                ((p2Reg.getUser() != null && p2Reg.getUser().getId().equals(perspectiveUserId)) ||
                        (p2Reg.getPartner() != null && p2Reg.getPartner().getId().equals(perspectiveUserId)));

        if (currentUserIsP1) {
            dto.setOpponentName(p2Name);
        } else if (currentUserIsP2) {
            dto.setOpponentName(p1Name);
        } else {
            // Если perspectiveUserId не является участником матча, и это не ожидалось (например, current user history),
            // это может быть ошибкой в логике findMatchesByUserId.
            // В общем случае (просмотр чужой истории), это нормально.
            dto.setOpponentName("N/A");
        }

        TournamentRegistration winnerReg = match.getWinner();
        if (winnerReg != null) { // Проверка, что победитель вообще есть
            dto.setWinnerName(winnerReg.getParticipantName());
            boolean winnerIsCurrentUser = (winnerReg.getUser() != null && winnerReg.getUser().getId().equals(perspectiveUserId)) ||
                    (winnerReg.getPartner() != null && winnerReg.getPartner().getId().equals(perspectiveUserId));
            dto.setCurrentUserWon(winnerIsCurrentUser);
        } else {
            dto.setWinnerName("N/A");
            // Для матчей без определенного победителя (например, еще не сыгран, или статус PENDING)
            // currentUserWon должно быть null или false. false уже было, оставим.
            dto.setCurrentUserWon(false); // или null, если false не подходит для не завершенных матчей
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public UserStatsDTO getCurrentUserStats() {
        User currentUser = userService.getAuthenticatedUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalStateException("Не удалось получить данные аутентифицированного пользователя.");
        }
        return calculateUserStats(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(Long userId) {
        if (userId == null) {
            throw new BadRequestException("ID пользователя не может быть null.");
        }
        // Проверка, существует ли пользователь
        User user = userRepository.findById(userId) // Сначала получаем пользователя
                .orElseThrow(() -> new NoSuchElementException("Пользователь с ID: " + userId + " не найден."));
        return calculateUserStats(user.getId()); // Передаем user.getId() для единообразия
    }

    private UserStatsDTO calculateUserStats(Long userId) {
        // userId здесь должен быть non-null и существующим
        List<Match> allUserMatches = matchRepository.findAllMatchesByUserId(userId);
        List<Match> wonMatches = matchRepository.findWonMatchesByUserId(userId);

        UserStatsDTO stats = new UserStatsDTO();

        long playedCount = allUserMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED || m.getStatus() == MatchStatus.WALKOVER)
                .count();
        stats.setTotalMatchesPlayed(playedCount);
        stats.setTotalWins(wonMatches.size());
        stats.setTotalLosses(playedCount > wonMatches.size() ? playedCount - wonMatches.size() : 0);

        Map<TournamentTier, Long> winsByTier = new EnumMap<>(TournamentTier.class);
        // Инициализируем карту нулями для всех существующих тиров
        Arrays.stream(TournamentTier.values()).forEach(tier -> winsByTier.put(tier, 0L));


        long singlesWinsCount = 0;
        long doublesWinsCount = 0;

        for (Match wonMatch : wonMatches) {
            Tournament tournament = wonMatch.getTournament();
            if (tournament == null) continue; // Пропускаем матчи без турнира, если такое возможно

            if (tournament.getTier() != null) {
                winsByTier.merge(tournament.getTier(), 1L, Long::sum);
            }
            if (tournament.getCategory() != null) {
                if (tournament.getCategory().isSingles()) {
                    singlesWinsCount++;
                } else if (tournament.getCategory().isDoubles()) {
                    doublesWinsCount++;
                }
            }
        }
        stats.setWinsByTier(winsByTier);
        stats.setSinglesWins(singlesWinsCount);
        stats.setDoublesWins(doublesWinsCount);

        long singlesMatchesPlayedCount = 0;
        long doublesMatchesPlayedCount = 0;

        for (Match match : allUserMatches) {
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
                continue;
            }
            Tournament tournament = match.getTournament();
            if (tournament == null) continue; // Пропускаем матчи без турнира

            if (tournament.getCategory() != null) {
                if (tournament.getCategory().isSingles()) {
                    singlesMatchesPlayedCount++;
                } else if (tournament.getCategory().isDoubles()) {
                    doublesMatchesPlayedCount++;
                }
            }
        }
        stats.setSinglesMatchesPlayed(singlesMatchesPlayedCount);
        stats.setDoublesMatchesPlayed(doublesMatchesPlayedCount);



        return stats;
    }
}