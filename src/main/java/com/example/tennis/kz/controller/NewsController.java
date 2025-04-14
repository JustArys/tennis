package com.example.tennis.kz.controller;

import com.example.tennis.kz.model.News;
import com.example.tennis.kz.model.response.CustomPageResponse;
import com.example.tennis.kz.service.NewServices;
import com.example.tennis.kz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource; // Используем InputStreamResource
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders; // Не используется для отображения, но может пригодиться
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// Импорты для работы с R2/S3 ответом
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
// import java.nio.file.Files; // Больше не нужен здесь
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final UserService userService;
    private final NewServices newServices;

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createNews(@RequestParam("file") MultipartFile file,
                                        @RequestParam String title,
                                        @RequestParam String description) { // Убрал throws IOException, т.к. сервис его обрабатывает или перевыбрасывает

        // --- Простая валидация на стороне контроллера (опционально) ---
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не должен быть пустым.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            // Можно оставить валидацию и здесь, и в сервисе для надежности
            return ResponseEntity.badRequest().body("Ошибка: Поддерживаются только файлы изображений (image/*).");
        }
        // --- Конец валидации ---

        try {
            News news = News.builder()
                    .title(title)
                    .description(description)
                    .author(userService.getAuthenticatedUser())
                    // ContentType будет установлен в сервисе перед сохранением
                    .build();

            // Сервис createNews теперь загружает файл в R2 и сохраняет новость
            News createdNews = newServices.createNews(news, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdNews); // Возвращаем 201 Created

        } catch (IOException e) {
            // Ловим возможные ошибки при загрузке файла сервисом
            System.err.println("Error creating news: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при создании новости: " + e.getMessage());
        } catch (RuntimeException e) {
            // Ловим другие ошибки, например, валидации типа файла из сервиса
            System.err.println("Error creating news: " + e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка при создании новости: " + e.getMessage());
        }
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<News> getNewsDetails(@PathVariable long newsId) { // Уточнил тип возвращаемого значения
        try {
            News news = newServices.findNews(newsId);
            return ResponseEntity.ok(news);
        } catch (NoSuchElementException e) {
            // Используем стандартный механизм Spring для возврата 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Новость не найдена", e);
        }
    }

    @PutMapping("/{newsId}/updateNews")
    public ResponseEntity<News> updateNews(@PathVariable long newsId, // Уточнил тип
                                           @RequestParam String title,
                                           @RequestParam String description) {
        try {
            News updatedNews = newServices.updateNews(newsId, title, description);
            return ResponseEntity.ok(updatedNews);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Новость для обновления не найдена", e);
        }
    }

    @DeleteMapping("/{newsId}")
    public ResponseEntity<Void> deleteNews(@PathVariable long newsId) { // Уточнил тип возвращаемого значения
        try {
            newServices.deleteNews(newsId);
            // Возвращаем стандартный ответ 204 No Content для успешного удаления
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) { // Если новость не найдена для удаления
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Новость для удаления не найдена", e);
        } catch (IOException e) { // Если ошибка при удалении файла из R2
            System.err.println("Error deleting news file from R2: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка при удалении файла новости", e);
        }
    }

    @GetMapping("/allNews")
    public ResponseEntity<CustomPageResponse<News>> findAllNews(@RequestParam(defaultValue = "1") int page, // Уточнил тип
                                                                @RequestParam(defaultValue = "10") int size) {

        // Убедимся, что номер страницы не отрицательный
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<News> newsPage = newServices.findAllNews(pageable);

        CustomPageResponse<News> response = new CustomPageResponse<>(
                newsPage.getNumber() + 1, // Возвращаем номер страницы как 1-based
                newsPage.getSize(),
                newsPage.getTotalElements(),
                newsPage.getContent()
        );
        return ResponseEntity.ok(response);
    }

    // --- Измененный эндпоинт для *отображения* изображения ---
    @GetMapping("/{newsId}/image")
    public ResponseEntity<Resource> getNewsImage(@PathVariable Long newsId) {
        ResponseInputStream<GetObjectResponse> responseInputStream = null;
        try {
            // 1. Вызываем метод сервиса, который возвращает ResponseInputStream
            responseInputStream = newServices.downloadNewsImage(newsId);

            // 2. Получаем метаданные (Content-Type, Content-Length) из ответа R2/S3
            GetObjectResponse s3ObjectResponse = responseInputStream.response();
            String contentType = s3ObjectResponse.contentType(); // Берем Content-Type из R2
            long contentLength = s3ObjectResponse.contentLength();

            // 3. Создаем ресурс Spring из InputStream'а
            InputStreamResource resource = new InputStreamResource(responseInputStream);

            // 4. Формируем ответ для ОТОБРАЖЕНИЯ изображения
            // Устанавливаем Content-Type и Content-Length, НЕ устанавливаем Content-Disposition=attachment
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(contentLength)
                    .body(resource);

        } catch (NoSuchElementException e) {
            // Если новость или изображение не найдены (сервис выбрасывает это исключение)
            System.err.println("Image not found for newsId " + newsId + ": " + e.getMessage());
            // Возвращаем 404 Not Found
            return ResponseEntity.notFound().build();
            // Альтернативно: throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Изображение не найдено", e);

        } catch (Exception e) { // Ловим другие возможные ошибки (например, от R2 при скачивании)
            System.err.println("Error serving image for news " + newsId + ": " + e.getMessage());
            // Важно: пытаемся закрыть стрим, если он был открыт, но произошла ошибка
            if (responseInputStream != null) {
                try {
                    responseInputStream.close();
                } catch (IOException ioException) {
                    System.err.println("Error closing R2 input stream after error: " + ioException.getMessage());
                }
            }
            // Возвращаем 500 Internal Server Error
            return ResponseEntity.internalServerError().build();
            // Альтернативно: throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка при загрузке изображения", e);
        }
        // Примечание: В случае успешного ответа (return ResponseEntity.ok()...), Spring позаботится о закрытии InputStream'а из InputStreamResource.
    }
}