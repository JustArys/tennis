package com.example.tennis.kz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

// Импортируем новое исключение
import com.example.tennis.kz.exception.BadRequestException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> buildErrorResponse(HttpStatus status, String message, List<String> details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }

    private Map<String, Object> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, message, null);
    }

    // --- НОВЫЙ ОБРАБОТЧИК ДЛЯ BadRequestException ---
    /**
     * Обработчик для кастомного BadRequestException.
     * Возвращает HTTP 400 Bad Request.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleCustomBadRequestException(BadRequestException ex, WebRequest request) {
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getDetails());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
    // ----------------------------------------------

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNoSuchElement(NoSuchElementException ex, WebRequest request) {
        Map<String, Object> body = buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации входных данных", details);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации параметров запроса", details);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = String.format("Параметр '%s' должен иметь тип '%s', но получено значение: '%s'",
                ex.getName(), ex.getRequiredType().getSimpleName(), ex.getValue());
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, WebRequest request) {
        String message = String.format("Пропущен обязательный параметр запроса: '%s' типа '%s'", ex.getParameterName(), ex.getParameterType());
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Этот обработчик для IllegalArgumentException остаётся.
    // Если BadRequestException НЕ наследуется от IllegalArgumentException, они будут обрабатываться раздельно.
    // Если бы наследовался, порядок был бы важен.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllOtherExceptions(Exception ex, WebRequest request) {
        System.err.println("An unexpected error occurred: " + ex.getClass().getName() + " - " + ex.getMessage());
        // В реальном приложении используй полноценный логгер (SLF4j, Logback, Log4j2)
        // ex.printStackTrace(); // Не выводи стектрейс напрямую в ответе или консоли продакшена без логгера

        Map<String, Object> body = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера. Пожалуйста, попробуйте позже.");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}