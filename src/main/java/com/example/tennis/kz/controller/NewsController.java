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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final UserService userService;
    private final NewServices newServices;

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNews(@RequestParam("file") MultipartFile file,
                                     @RequestParam String title,
                                     @RequestParam String description) throws IOException {
        News news = News.builder()
                .title(title)
                .description(description)
                .author(userService.getAuthenticatedUser())
                .build();
        return ResponseEntity.ok(newServices.createNews(news, file));
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<?> getNews(@PathVariable long newsId) {
        return ResponseEntity.ok(newServices.findNews(newsId));
    }

    @PutMapping("/{newsId}/updateNews")
    public ResponseEntity<?> updateNews(@PathVariable long newsId, @RequestParam String title, @RequestParam String description) {
        return ResponseEntity.ok(newServices.updateNews(newsId, title, description));
    }

    @DeleteMapping("/{newsId}")
    public ResponseEntity<?> deleteNews(@PathVariable long newsId) throws IOException {
        newServices.deleteNews(newsId);
        return ResponseEntity.ok(ResponseEntity.noContent().build());
    }

    @GetMapping("/allNews")
    public ResponseEntity<?> findAllPartner(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<News> news = newServices.findAllNews(pageable);
        return ResponseEntity.ok(new CustomPageResponse<>(news.getNumber() + 1, news.getSize(), news.getTotalElements(), news.getContent()));
    }

    @GetMapping("/{newsId}/image") // Новый эндпоинт для скачивания
    public ResponseEntity<Resource> downloadAssignmentFile(@PathVariable Long newsId) {
        try {
            Resource file = newServices.downloadNews(newsId);

            if (file == null) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream"; // По умолчанию
            // Можно добавить логику для определения Content-Type на основе расширения файла

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                    .body(file);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
