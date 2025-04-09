package com.example.tennis.kz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException; // Для ошибок валидации @Valid
import org.springframework.web.bind.MissingServletRequestParameterException; // Для пропущенных параметров запроса
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException; // Для ошибок типа параметров

import jakarta.validation.ConstraintViolationException; // Для ошибок валидации параметров/пути
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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



    // Обработка исключения, когда элемент не найден (обычно лучше 404, но если нужен 400...)
    // ВАЖНО: NoSuchElementException обычно СЕМАНТИЧЕСКИ правильнее маппить на 404 Not Found
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNoSuchElement(NoSuchElementException ex, WebRequest request) {
        // Если ты *точно* хочешь 400 для этого:
        // Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        // return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);

        // Рекомендуемый вариант - 404 Not Found:
        Map<String, Object> body = buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Обработка ошибок валидации @Valid в теле запроса (RequestBody)
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

    // Обработка ошибок валидации параметров запроса (@RequestParam, @PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации параметров запроса", details);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Обработка ошибок типа аргументов (например, строка вместо числа в @PathVariable)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String message = String.format("Параметр '%s' должен иметь тип '%s', но получено значение: '%s'",
                ex.getName(), ex.getRequiredType().getSimpleName(), ex.getValue());
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Обработка пропущенных обязательных параметров запроса
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, WebRequest request) {
        String message = String.format("Пропущен обязательный параметр запроса: '%s' типа '%s'", ex.getParameterName(), ex.getParameterType());
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Обработка общих ошибок некорректного запроса (можно добавить HttpMessageNotReadableException для невалидного JSON и т.д.)
    @ExceptionHandler(IllegalArgumentException.class) // Ловит и другие подклассы, если нет более специфичного обработчика
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> body = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Обработчик для IOException - ОСТОРОЖНО!
    // IOException может быть и ошибкой сервера (диск недоступен), и ошибкой клиента (битый файл).
    // Маппинг его всегда на 400 может скрыть проблемы сервера.
    // @ExceptionHandler(IOException.class)
    // public ResponseEntity<Object> handleIOException(IOException ex, WebRequest request) {
    //     // Логируем ошибку для диагностики
    //     System.err.println("IOException occurred: " + ex.getMessage());
    //     // Можно попытаться проанализировать тип IOException, но чаще лучше вернуть 500
    //     Map<String, Object> body = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка ввода-вывода при обработке файла");
    //     return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    // }


    // --- Общий обработчик для непредвиденных ошибок (возвращает 500) ---
    // Важно иметь его, чтобы не вылетали необработанные 500 со стектрейсами
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllOtherExceptions(Exception ex, WebRequest request) {
        // Важно логировать такие ошибки для разработчиков
        System.err.println("An unexpected error occurred: " + ex.getMessage());
        ex.printStackTrace(); // В реальном приложении используй логгер (SLF4j, Logback, Log4j2)

        Map<String, Object> body = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера. Пожалуйста, попробуйте позже.");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}