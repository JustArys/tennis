package com.example.tennis.kz.model;

public enum RegistrationStatus {
    /**
     * Регистрация ожидает подтверждения партнёра (если турнир парный, партнер ещё не подтвердил).
     */
    PENDING_PARTNER,

    /**
     * Полная регистрация (одиночный или партнёр согласился).
     */
    REGISTERED,

    /**
     * Партнёр отклонил приглашение или регистрация отменена.
     */
    REJECTED,
    CANCELED
}
