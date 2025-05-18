package com.example.tennis.kz.service; // или com.example.tennis.kz.service.impl

import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.model.TournamentRegistration;
import com.example.tennis.kz.model.response.MatchDto;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentQueryService { // Класс вместо интерфейса + реализации

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

    public List<MatchDto> getTournamentBracket(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new EntityNotFoundException("Tournament not found with ID: " + tournamentId);
        }
        List<Match> matches = matchRepository.findByTournamentIdOrderByRoundNumberAscMatchNumberInBracketAsc(tournamentId);

        return matches.stream()
                .map(this::convertToMatchDTO)
                .collect(Collectors.toList());
    }

    private MatchDto convertToMatchDTO(Match match) {
        if (match == null) {
            return null;
        }

        MatchDto.MatchDtoBuilder builder = MatchDto.builder()
                .id(match.getId())
                .roundNumber(match.getRoundNumber())
                .roundName(match.getRoundName()) // Вызов @Transient метода внутри транзакции
                .matchNumberInBracket(match.getMatchNumberInBracket())
                .score(match.getScore())
                .status(match.getStatus())
                .scheduledTime(match.getScheduledTime())
                .completedTime(match.getCompletedTime())
                .nextMatchSlot(match.getNextMatchSlot());

        if (match.getNextMatch() != null) {
            builder.nextMatchId(match.getNextMatch().getId()); // Доступ к ID лениво загруженного nextMatch
        }

        TournamentRegistration p1Reg = match.getParticipant1();
        if (p1Reg != null) {
            builder.participant1RegistrationId(p1Reg.getId())
                    .participant1Name(p1Reg.getParticipantName()) // Вызов @Transient метода
                    .participant1SeedNumber(p1Reg.getSeedNumber());
            if (p1Reg.getUser() != null) {
                builder.participant1Player1UserId(p1Reg.getUser().getId());
            }
            if (p1Reg.getPartner() != null) {
                builder.participant1Player2UserId(p1Reg.getPartner().getId());
            }
        }

        TournamentRegistration p2Reg = match.getParticipant2();
        if (p2Reg != null) {
            builder.participant2RegistrationId(p2Reg.getId())
                    .participant2Name(p2Reg.getParticipantName()) // Вызов @Transient метода
                    .participant2SeedNumber(p2Reg.getSeedNumber());
            if (p2Reg.getUser() != null) {
                builder.participant2Player1UserId(p2Reg.getUser().getId());
            }
            if (p2Reg.getPartner() != null) {
                builder.participant2Player2UserId(p2Reg.getPartner().getId());
            }
        }

        TournamentRegistration winnerReg = match.getWinner();
        if (winnerReg != null) {
            builder.winnerRegistrationId(winnerReg.getId())
                    .winnerName(winnerReg.getParticipantName()); // Вызов @Transient метода
        }

        return builder.build();
    }
}