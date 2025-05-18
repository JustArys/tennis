package com.example.tennis.kz.repository;

import com.example.tennis.kz.model.Match;
import com.example.tennis.kz.model.Tournament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentIdOrderByRoundNumberAscMatchNumberInBracketAsc(Long tournamentId);
    Optional<Match> findByTournamentAndRoundNumberAndMatchNumberInBracket(Tournament tournament, int roundNumber, int matchNumberInBracket);

    @Query("SELECT m FROM Match m WHERE " +
            "(m.participant1.user.id = :userId OR m.participant2.user.id = :userId " +
            "OR (m.participant1.partner IS NOT NULL AND m.participant1.partner.id = :userId) " + // Пользователь - партнер в первой регистрации
            "OR (m.participant2.partner IS NOT NULL AND m.participant2.partner.id = :userId)) " + // Пользователь - партнер во второй регистрации
            "ORDER BY m.completedTime DESC NULLS LAST, m.scheduledTime DESC NULLS LAST, m.id DESC")
    Page<Match> findMatchesByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Находит ВСЕ матчи (без пагинации), в которых участвовал пользователь.
     * Используется для агрегации статистики.
     */
    @Query("SELECT m FROM Match m WHERE " +
            "(m.participant1.user.id = :userId OR m.participant2.user.id = :userId " +
            "OR (m.participant1.partner IS NOT NULL AND m.participant1.partner.id = :userId) " +
            "OR (m.participant2.partner IS NOT NULL AND m.participant2.partner.id = :userId))")
    List<Match> findAllMatchesByUserId(@Param("userId") Long userId);


    /**
     * Находит все матчи, ВЫИГРАННЫЕ пользователем (как основной игрок или как партнер).
     * Это ключевой метод, который вызывал ошибку. Убедись, что @Query здесь есть.
     */
    @Query("SELECT m FROM Match m WHERE m.winner IS NOT NULL AND (" + // Убедимся, что победитель определен
            "m.winner.user.id = :userId " + // Пользователь - основной игрок в выигравшей регистрации
            "OR (m.winner.partner IS NOT NULL AND m.winner.partner.id = :userId)" + // Пользователь - партнер в выигравшей регистрации
            ")")
    List<Match> findWonMatchesByUserId(@Param("userId") Long userId);
}