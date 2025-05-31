package com.example.tennis.kz.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Пользовательское исключение для представления HTTP 400 Bad Request.
 * Используется, когда запрос не может быть обработан из-за ошибки клиента
 * (например, неверные данные, нарушены бизнес-правила).
 */
@Getter
public class BadRequestException extends RuntimeException {

    private final List<String> details;

    public BadRequestException(String message) {
        super(message);
        this.details = Collections.emptyList();
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.details = Collections.emptyList();
    }

    public BadRequestException(String message, List<String> details) {
        super(message);
        this.details = details != null ? Collections.unmodifiableList(details) : Collections.emptyList();
    }

    public BadRequestException(String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.details = details != null ? Collections.unmodifiableList(details) : Collections.emptyList();
    }

}