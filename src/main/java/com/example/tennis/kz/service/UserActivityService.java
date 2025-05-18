package com.example.tennis.kz.service;

import com.example.tennis.kz.model.*;
import com.example.tennis.kz.model.response.MatchHistoryDTO;
import com.example.tennis.kz.model.response.UserStatsDTO;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final UserService userService; // Для получения текущего аутентифицированного пользователя

    @Transactional(readOnly = true)
    public Page<MatchHistoryDTO> getCurrentUserMatchHistory(Pageable pageable) {
        User currentUser = userService.getAuthenticatedUser();
        Page<Match> matchesPage = matchRepository.findMatchesByUserId(currentUser.getId(), pageable);
        return matchesPage.map(match -> convertToMatchHistoryDTO(match, currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public Page<MatchHistoryDTO> getUserMatchHistory(Long userId, Pageable pageable) {
        // Проверка, существует ли пользователь (опционально, если userId всегда валиден из другого источника)
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        Page<Match> matchesPage = matchRepository.findMatchesByUserId(userId, pageable);
        return matchesPage.map(match -> convertToMatchHistoryDTO(match, userId));
    }

    private MatchHistoryDTO convertToMatchHistoryDTO(Match match, Long currentUserId) {
        MatchHistoryDTO dto = new MatchHistoryDTO();
        dto.setMatchId(match.getId());

        Tournament tournament = match.getTournament(); // Избегаем повторных вызовов getTournament()
        if (tournament != null) {
            dto.setTournamentName(tournament.getDescription());
            dto.setTournamentTier(tournament.getTier());
            dto.setTournamentCategory(tournament.getCategory());
        }
        dto.setRoundName(match.getRoundName());
        dto.setMatchDate(match.getCompletedTime() != null ? match.getCompletedTime() : match.getScheduledTime());

        TournamentRegistration p1Reg = match.getParticipant1();
        TournamentRegistration p2Reg = match.getParticipant2();

        String p1Name = (p1Reg != null && p1Reg.getUser() != null) ? p1Reg.getParticipantName() : "N/A";
        String p2Name = (p2Reg != null && p2Reg.getUser() != null) ? p2Reg.getParticipantName() : "N/A";
        dto.setParticipant1Name(p1Name);
        dto.setParticipant2Name(p2Name);
        dto.setScore(match.getScore());

        // Определяем, является ли currentUserId одним из участников
        boolean currentUserIsP1 = p1Reg != null && p1Reg.getUser() != null &&
                (p1Reg.getUser().getId().equals(currentUserId) ||
                        (p1Reg.getPartner() != null && p1Reg.getPartner().getId().equals(currentUserId)));
        boolean currentUserIsP2 = p2Reg != null && p2Reg.getUser() != null &&
                (p2Reg.getUser().getId().equals(currentUserId) ||
                        (p2Reg.getPartner() != null && p2Reg.getPartner().getId().equals(currentUserId)));

        if (currentUserIsP1) {
            dto.setOpponentName(p2Name);
        } else if (currentUserIsP2) {
            dto.setOpponentName(p1Name);
        } else {
            // Эта ситуация не должна возникать, если findMatchesByUserId работает корректно
            dto.setOpponentName("N/A (Error determining opponent)");
        }

        TournamentRegistration winnerReg = match.getWinner();
        if (winnerReg != null && winnerReg.getUser() != null) {
            dto.setWinnerName(winnerReg.getParticipantName());
            boolean winnerIsCurrentUser = winnerReg.getUser().getId().equals(currentUserId) ||
                    (winnerReg.getPartner() != null && winnerReg.getPartner().getId().equals(currentUserId));
            dto.setCurrentUserWon(winnerIsCurrentUser);
        } else {
            dto.setWinnerName("N/A"); // Если победитель еще не определен или матч не завершен
            dto.setCurrentUserWon(false);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public UserStatsDTO getCurrentUserStats() {
        User currentUser = userService.getAuthenticatedUser();
        return calculateUserStats(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(Long userId) {
        // Проверка, существует ли пользователь
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        return calculateUserStats(userId);
    }

    private UserStatsDTO calculateUserStats(Long userId) {
        List<Match> allUserMatches = matchRepository.findAllMatchesByUserId(userId);
        List<Match> wonMatches = matchRepository.findWonMatchesByUserId(userId);

        UserStatsDTO stats = new UserStatsDTO();

        long playedCount = allUserMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED || m.getStatus() == MatchStatus.WALKOVER)
                .count();
        stats.setTotalMatchesPlayed(playedCount);
        stats.setTotalWins(wonMatches.size()); // Размер списка выигранных матчей
        stats.setTotalLosses(playedCount - wonMatches.size());

        Map<TournamentTier, Long> winsByTier = new EnumMap<>(TournamentTier.class);
        for (TournamentTier tier : TournamentTier.values()) { // Инициализируем карту нулями
            winsByTier.put(tier, 0L);
        }

        long singlesWinsCount = 0;
        long doublesWinsCount = 0;
        long singlesMatchesPlayedCount = 0;
        long doublesMatchesPlayedCount = 0;

        for (Match wonMatch : wonMatches) {
            Tournament tournament = wonMatch.getTournament();
            if (tournament != null && tournament.getTier() != null) {
                winsByTier.merge(tournament.getTier(), 1L, Long::sum);
            }
            if (tournament != null && tournament.getCategory() != null) {
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

        for (Match match : allUserMatches) {
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.WALKOVER) {
                continue; // Считаем только завершенные матчи
            }
            Tournament tournament = match.getTournament();
            if (tournament != null && tournament.getCategory() != null) {
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
