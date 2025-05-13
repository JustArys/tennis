package com.example.tennis.kz.model;

public enum MatchStatus {
    PENDING_PARTICIPANTS, // Ожидает определения участников (из предыдущего раунда)
    SCHEDULED,            // Участники определены, матч назначен
    IN_PROGRESS,          // Матч идет (опционально, для live-трекинга)
    COMPLETED,            // Матч завершен
    WALKOVER              // Техническая победа (неявка одного из участников)
}