package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Импорт
import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.model.TournamentRegistration;
import com.example.tennis.kz.model.response.MatchDto;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.TournamentRepository;
// import jakarta.persistence.EntityNotFoundException; // Заменяем
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException; // Импорт
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentQueryService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

    public List<MatchDto> getTournamentBracket(Long tournamentId) {
        if (tournamentId == null) {
            throw new BadRequestException("ID турнира не может быть null.");
        }
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new NoSuchElementException("Турнир с ID: " + tournamentId + " не найден.");
        }
        List<Match> matches = matchRepository.findByTournamentIdOrderByRoundNumberAscMatchNumberInBracketAsc(tournamentId);

        return matches.stream()
                .map(this::convertToMatchDTO) // convertToMatchDTO может вернуть null, это будет отфильтровано если необходимо выше
                .collect(Collectors.toList());
    }

    private MatchDto convertToMatchDTO(Match match) {
        if (match == null) {
            // Это не должно происходить, если список matches из репозитория не содержит null.
            // Если это все же произошло, это может указывать на проблему с данными.
            // Вместо возврата null можно было бы выбросить IllegalStateException,
            // но для DTO-маппера возврат null при null-входе - обычная практика.
            return null;
        }

        MatchDto.MatchDtoBuilder builder = MatchDto.builder()
                .id(match.getId())
                .roundNumber(match.getRoundNumber())
                .roundName(match.getRoundName())
                .matchNumberInBracket(match.getMatchNumberInBracket())
                .score(match.getScore())
                .status(match.getStatus())
                .scheduledTime(match.getScheduledTime())
                .completedTime(match.getCompletedTime())
                .nextMatchSlot(match.getNextMatchSlot());

        if (match.getNextMatch() != null) {
            builder.nextMatchId(match.getNextMatch().getId());
        }

        TournamentRegistration p1Reg = match.getParticipant1();
        if (p1Reg != null) {
            builder.participant1RegistrationId(p1Reg.getId())
                    .participant1Name(p1Reg.getParticipantName())
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
                    .participant2Name(p2Reg.getParticipantName())
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
                    .winnerName(winnerReg.getParticipantName());
        }

        return builder.build();
    }
}