package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentIdOrderByRoundNumberAscMatchNumberInBracketAsc(Long tournamentId);
    Optional<Match> findByTournamentAndRoundNumberAndMatchNumberInBracket(Tournament tournament, int roundNumber, int matchNumberInBracket);
}