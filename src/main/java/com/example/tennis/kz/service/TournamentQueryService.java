package com.example.tennis.kz.service; // или com.example.tennis.kz.service.impl

import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentQueryService { // Класс вместо интерфейса + реализации

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

    public List<Match> getTournamentBracket(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new EntityNotFoundException("Tournament not found with ID: " + tournamentId);
        }
        return matchRepository.findByTournamentIdOrderByRoundNumberAscMatchNumberInBracketAsc(tournamentId);
    }
}