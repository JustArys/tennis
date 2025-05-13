package com.example.tennis.kz.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore // Чтобы избежать циклической зависимости при сериализации из Tournament
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Column(nullable = false)
    private Integer roundNumber; // Номер раунда (1 = первый круг, ..., N = финал)

    @Column(nullable = false)
    private Integer matchNumberInBracket; // Уникальный порядковый номер матча в сетке (для связывания и отрисовки)

    // Ссылки на РЕГИСТРАЦИЮ участников
    @ManyToOne(fetch = FetchType.EAGER) // EAGER, т.к. информация об участниках матча нужна часто
    @JoinColumn(name = "participant1_reg_id")
    private TournamentRegistration participant1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "participant2_reg_id")
    private TournamentRegistration participant2;

    // Ссылка на РЕГИСТРАЦИЮ победителя
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winner_reg_id")
    private TournamentRegistration winner;

    // Ссылка на следующий матч, куда проходит победитель
    // Используется для построения дерева сетки
    @JsonIgnore // Чтобы избежать глубокой рекурсии при сериализации
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch; // Матч, в который переходит победитель

    // В какой слот (1 или 2) следующего матча идет победитель
    // Полезно при автоматическом продвижении по сетке
    private Integer nextMatchSlot;

    private String score; // Например, "6-4, 7-5" или "W/O" (walkover)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDING_PARTICIPANTS;

    private LocalDateTime scheduledTime; // Планируемое время начала матча
    private LocalDateTime completedTime; // Фактическое время завершения матча

    // --- Вспомогательные методы (не мапятся, для DTO или фронтенда) ---
    @Transient
    public String getRoundName() {
        if (this.tournament == null || this.roundNumber == null) return "N/A";
        int totalRounds = this.tournament.getTotalRounds();
        if (this.roundNumber == totalRounds) return "Финал";
        if (this.roundNumber == totalRounds - 1) return "Полуфинал";
        if (this.roundNumber == totalRounds - 2) return "Четвертьфинал";
        // Для более ранних раундов
        int participantsPower = (int) Math.pow(2, totalRounds - this.roundNumber + 1);
        return "1/" + (participantsPower / 2) + " финала";
    }

    @Transient
    public String getParticipant1Name() {
        return (participant1 != null) ? participant1.getParticipantName() : "TBD";
    }

    @Transient
    public String getParticipant2Name() {
        return (participant2 != null) ? participant2.getParticipantName() : "TBD";
    }

    @Transient
    public String getWinnerName() {
        return (winner != null) ? winner.getParticipantName() : "";
    }

    // Позиция в сетке для отрисовки (верхняя/нижняя половина)
    // true - верхняя, false - нижняя. Упрощенный вариант для первого раунда.
    // Для точного определения положения в сложных сетках может потребоваться более сложная логика
    // или передача координат от сервиса генерации сетки.
    @Transient
    public boolean isInUpperHalf() {
        if (this.tournament == null || this.matchNumberInBracket == null || this.roundNumber == null) {
            return true; // Default or throw exception
        }
        // Для первого раунда:
        if (this.roundNumber == 1) {
            int matchesInFirstRound = this.tournament.getMaxParticipants() / 2;
            return this.matchNumberInBracket <= matchesInFirstRound / 2;
        }
        // Для последующих раундов логика сложнее, т.к. нужно отслеживать путь от первого раунда
        // Это поле лучше вычислять при генерации DTO для фронтенда, зная всю структуру сетки.
        // Здесь упрощенный пример, который может быть не всегда точен для отрисовки > 1 раунда.
        // Обычно клиентская библиотека для отрисовки сетки сама располагает матчи.
        return true;
    }
}