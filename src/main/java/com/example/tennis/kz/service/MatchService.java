package com.example.tennis.kz.service; // или com.example.tennis.kz.service.impl

import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.model.MatchStatus;
import com.example.tennis.kz.model.TournamentRegistration;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;

    @Transactional
    public Match recordResult(Long matchId, Long winnerRegistrationId, String score) {
        // System.out.println("Recording result for match ID: " + matchId + ", winnerRegID: " + winnerRegistrationId + ", score: " + score);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with ID: " + matchId));

        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER) {
            throw new IllegalStateException("Match already completed or resulted in a walkover.");
        }
        if (match.getParticipant1() == null || match.getParticipant2() == null) {
            throw new IllegalStateException("Cannot record result, participants not fully set for match.");
        }

        TournamentRegistration winnerReg = tournamentRegistrationRepository.findById(winnerRegistrationId)
                .orElseThrow(() -> new EntityNotFoundException("Winner registration not found with ID: " + winnerRegistrationId));

        if (!winnerReg.equals(match.getParticipant1()) && !winnerReg.equals(match.getParticipant2())) {
            throw new IllegalArgumentException("Declared winner is not a participant in this match.");
        }

        match.setWinner(winnerReg);
        match.setScore(score);
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedTime(LocalDateTime.now());

        advanceWinner(match, winnerReg);

        // System.out.println("Match ID " + matchId + " result recorded successfully.");
        return matchRepository.save(match);
    }

    @Transactional
    public Match recordWalkover(Long matchId, Long winnerRegistrationId) {
        // System.out.println("Recording walkover for match ID: " + matchId + ", winnerRegID: " + winnerRegistrationId);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found with ID: " + matchId));

        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER) {
            throw new IllegalStateException("Match already completed or resulted in a walkover.");
        }
        if (match.getParticipant1() == null || match.getParticipant2() == null) {
            throw new IllegalStateException("Cannot record walkover, participants not fully set for match.");
        }

        TournamentRegistration winnerReg = tournamentRegistrationRepository.findById(winnerRegistrationId)
                .orElseThrow(() -> new EntityNotFoundException("Winner registration not found with ID: " + winnerRegistrationId));

        if (!winnerReg.equals(match.getParticipant1()) && !winnerReg.equals(match.getParticipant2())) {
            throw new IllegalArgumentException("Declared winner (by walkover) is not a participant in this match.");
        }

        match.setWinner(winnerReg);
        match.setScore("W/O"); // Walkover
        match.setStatus(MatchStatus.WALKOVER);
        match.setCompletedTime(LocalDateTime.now());

        advanceWinner(match, winnerReg);
        // System.out.println("Match ID " + matchId + " walkover recorded successfully.");
        return matchRepository.save(match);
    }

    private void advanceWinner(Match completedMatch, TournamentRegistration winner) {
        Match nextMatch = completedMatch.getNextMatch();
        if (nextMatch != null) {
            // System.out.println("Advancing winner of match " + completedMatch.getId() + " to next match " + nextMatch.getId());
            Match nextMatchEntity = matchRepository.findById(nextMatch.getId()).orElse(null);
            if(nextMatchEntity == null) {
                // System.err.println("Error: Next match with ID " + nextMatch.getId() + " not found for completed match " + completedMatch.getId());
                return; // Это не должно происходить при корректной структуре сетки
            }

            Integer slot = completedMatch.getNextMatchSlot();
            if (slot == null) {
                // System.err.println("Error: nextMatchSlot is null for match " + completedMatch.getId());
                return; // Не можем определить слот
            }

            if (slot == 1) {
                nextMatchEntity.setParticipant1(winner);
            } else if (slot == 2) {
                nextMatchEntity.setParticipant2(winner);
            }

            if (nextMatchEntity.getParticipant1() != null && nextMatchEntity.getParticipant2() != null) {
                nextMatchEntity.setStatus(MatchStatus.SCHEDULED);
            }
            matchRepository.save(nextMatchEntity);
            // System.out.println("Winner advanced to slot " + slot + " of match " + nextMatchEntity.getId() + ". Next match status: " + nextMatchEntity.getStatus());
        } else {
            // System.out.println("Match " + completedMatch.getId() + " was the final match or next match link is missing. Winner is the tournament champion.");
        }
    }
}