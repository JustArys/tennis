package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.News;
import com.example.tennis.kz.model.response.CustomPageResponse;
import com.example.tennis.kz.service.NewServices;
import com.example.tennis.kz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus; // Импортируем HttpStatus
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException; // Для удобства выбрасывания ошибок

import java.io.IOException;
import java.nio.file.Files; // Для определения Content-Type, если он не сохранен
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final UserService userService;
    private final NewServices newServices;

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createNews(@RequestParam("file") MultipartFile file, // Переименовал метод для ясности
                                        @RequestParam String title,
                                        @RequestParam String description) throws IOException {


        News news = News.builder()
                .title(title)
                .description(description)
                .author(userService.getAuthenticatedUser())
                // Важно: Рекомендуется сохранять Content-Type файла вместе с новостью в сервисе/базе данных
                // .contentType(contentType) // Пример, если модель News имеет поле contentType
                .build();

        // Предполагаем, что createNews теперь может сохранять и contentType
        return ResponseEntity.ok(newServices.createNews(news, file));
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<?> getNewsDetails(@PathVariable long newsId) { // Переименовал метод для ясности
        return ResponseEntity.ok(newServices.findNews(newsId));
    }

    @PutMapping("/{newsId}/updateNews")
    public ResponseEntity<?> updateNews(@PathVariable long newsId, @RequestParam String title, @RequestParam String description) {
        return ResponseEntity.ok(newServices.updateNews(newsId, title, description));
    }

    @DeleteMapping("/{newsId}")
    public ResponseEntity<?> deleteNews(@PathVariable long newsId) throws IOException {
        newServices.deleteNews(newsId);
        // Возвращаем стандартный ответ 204 No Content для успешного удаления без тела ответа
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/allNews")
    public ResponseEntity<?> findAllNews(@RequestParam(defaultValue = "1") int page, // Переименовал параметр метода для ясности
                                         @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<News> newsPage = newServices.findAllNews(pageable); // Переименовал переменную для ясности
        // Создаем кастомный ответ для пагинации
        CustomPageResponse<News> response = new CustomPageResponse<>(
                newsPage.getNumber() + 1,
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getContent()
        );
        return ResponseEntity.ok(response);
    }

    // Измененный эндпоинт для *отображения* изображения
    @GetMapping("/{newsId}/image")
    public ResponseEntity<Resource> getNewsImage(@PathVariable Long newsId) {
        try {
            Resource file = newServices.downloadNews(newsId);
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = newServices.findNews(newsId).getImageContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // Запасной вариант
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(file);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error serving image for news " + newsId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}