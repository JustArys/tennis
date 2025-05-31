package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Импорт
import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.model.MatchStatus;
import com.example.tennis.kz.model.TournamentRegistration;
import com.example.tennis.kz.repository.MatchRepository;
import com.example.tennis.kz.repository.TournamentRegistrationRepository;
// import jakarta.persistence.EntityNotFoundException; // Заменяем на NoSuchElementException
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException; // Импорт

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;

    @Transactional
    public Match recordResult(Long matchId, Long winnerRegistrationId, String score) {
        if (matchId == null || winnerRegistrationId == null) {
            throw new BadRequestException("ID матча и ID победителя не могут быть null.");
        }
        if (score == null || score.trim().isEmpty()) {
            throw new BadRequestException("Счет матча не может быть пустым.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NoSuchElementException("Матч с ID: " + matchId + " не найден."));

        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER) {
            throw new BadRequestException("Матч уже завершен или закончился техническим поражением.");
        }
        if (match.getParticipant1() == null || match.getParticipant2() == null) {
            // Это может также указывать на некорректное состояние сетки, возможно IllegalStateException (ведущий к 500) был бы уместнее,
            // но если это результат преждевременного запроса клиента, то BadRequestException тоже подходит.
            throw new BadRequestException("Невозможно записать результат, участники матча определены не полностью.");
        }

        TournamentRegistration winnerReg = tournamentRegistrationRepository.findById(winnerRegistrationId)
                .orElseThrow(() -> new NoSuchElementException("Регистрация победителя с ID: " + winnerRegistrationId + " не найдена."));

        if (!winnerReg.equals(match.getParticipant1()) && !winnerReg.equals(match.getParticipant2())) {
            throw new BadRequestException("Заявленный победитель не является участником этого матча.");
        }

        match.setWinner(winnerReg);
        match.setScore(score);
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedTime(LocalDateTime.now());

        matchRepository.save(match); // Сохраняем результат матча перед продвижением
        advanceWinner(match, winnerReg); // Продвигаем победителя

        return match; // Возвращаем обновленный матч (уже сохраненный)
    }

    @Transactional
    public Match recordWalkover(Long matchId, Long winnerRegistrationId) {
        if (matchId == null || winnerRegistrationId == null) {
            throw new BadRequestException("ID матча и ID победителя (по тех. поражению) не могут быть null.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NoSuchElementException("Матч с ID: " + matchId + " не найден."));

        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.WALKOVER) {
            throw new BadRequestException("Матч уже завершен или закончился техническим поражением.");
        }
        if (match.getParticipant1() == null || match.getParticipant2() == null) {
            throw new BadRequestException("Невозможно записать техническое поражение, участники матча определены не полностью.");
        }

        TournamentRegistration winnerReg = tournamentRegistrationRepository.findById(winnerRegistrationId)
                .orElseThrow(() -> new NoSuchElementException("Регистрация победителя (по тех. поражению) с ID: " + winnerRegistrationId + " не найдена."));

        if (!winnerReg.equals(match.getParticipant1()) && !winnerReg.equals(match.getParticipant2())) {
            throw new BadRequestException("Заявленный победитель (по тех. поражению) не является участником этого матча.");
        }

        match.setWinner(winnerReg);
        match.setScore("W/O"); // Walkover
        match.setStatus(MatchStatus.WALKOVER);
        match.setCompletedTime(LocalDateTime.now());

        matchRepository.save(match); // Сохраняем результат матча перед продвижением
        advanceWinner(match, winnerReg); // Продвигаем победителя

        return match; // Возвращаем обновленный матч (уже сохраненный)
    }

    private void advanceWinner(Match completedMatch, TournamentRegistration winner) {
        Match nextMatch = completedMatch.getNextMatch();
        if (nextMatch != null) {
            // Используем ID для получения актуальной сущности из БД
            Match nextMatchEntity = matchRepository.findById(nextMatch.getId())
                    .orElseThrow(() -> new IllegalStateException("Нарушение целостности сетки: следующий матч с ID " + nextMatch.getId() + " не найден для завершенного матча " + completedMatch.getId()));

            Integer slot = completedMatch.getNextMatchSlot();
            if (slot == null) {
                throw new IllegalStateException("Нарушение целостности сетки: nextMatchSlot is null для матча " + completedMatch.getId());
            }

            if (slot == 1) {
                nextMatchEntity.setParticipant1(winner);
            } else if (slot == 2) {
                nextMatchEntity.setParticipant2(winner);
            } else {
                // Это не должно произойти, если слот всегда 1 или 2
                throw new IllegalStateException("Нарушение целостности сетки: некорректный номер слота " + slot + " для матча " + completedMatch.getId());
            }

            if (nextMatchEntity.getParticipant1() != null && nextMatchEntity.getParticipant2() != null) {
                nextMatchEntity.setStatus(MatchStatus.SCHEDULED);
            } else {
                // Если только один участник известен, статус остается PENDING_PARTICIPANTS
                // или можно ввести статус PENDING_OPPONENT
                nextMatchEntity.setStatus(MatchStatus.PENDING_PARTICIPANTS);
            }
            matchRepository.save(nextMatchEntity);
        }
    }
}