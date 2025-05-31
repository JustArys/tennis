package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Импорт
import com.example.tennis.kz.model.News;
import com.example.tennis.kz.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NewServices { // Предполагается NewsServices

    private final NewsRepository newsRepository;
    private final R2StorageService r2StorageService;

    @Transactional
    public News createNews(News news, MultipartFile file) throws IOException {
        if (news == null) {
            throw new BadRequestException("Объект новости не может быть null.");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл для новости не может быть пустым.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Ошибка: Поддерживаются только файлы изображений (image/*). Получен тип: " + contentType);
        }
        if (news.getTitle() == null || news.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Заголовок новости не может быть пустым.");
        }
        // Дополнительные проверки для news.getDescription() если необходимо

        String imageKey = r2StorageService.uploadFile(file);
        news.setImage(imageKey);
        news.setImageContentType(contentType);

        return newsRepository.save(news);
    }

    @Transactional
    public void deleteNews(Long id) throws IOException {
        if (id == null) {
            throw new BadRequestException("ID новости для удаления не может быть null.");
        }
        News news = findNews(id); // findNews выбросит NoSuchElementException если новость не найдена
        String imageKey = news.getImage();

        if (imageKey != null && !imageKey.isEmpty()) {
            r2StorageService.deleteFile(imageKey);
        }
        newsRepository.delete(news);
    }

    public News findNews(Long id) {
        if (id == null) {
            throw new BadRequestException("ID новости не может быть null.");
        }
        return newsRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("Новость с ID '%d' не найдена.", id)));
    }

    public ResponseInputStream<GetObjectResponse> downloadNewsImage(Long newsId) {
        News news = findNews(newsId); // findNews содержит проверки
        String imageKey = news.getImage();

        if (imageKey == null || imageKey.isEmpty()) {
            throw new NoSuchElementException(String.format("Новость с ID '%d' не имеет связанного изображения.", newsId));
        }

        ResponseInputStream<GetObjectResponse> responseStream = r2StorageService.downloadFile(imageKey);

        if (responseStream == null) {
            // Это указывает на несоответствие (ключ есть, файла нет в R2) или ошибку в R2StorageService
            throw new IllegalStateException(String.format("Не удалось загрузить изображение с ключом '%s' для новости ID '%d'. Возможно, файл отсутствует в хранилище.", imageKey, newsId));
        }
        return responseStream;
    }

    @Transactional
    public News updateNews(Long newsId, String title, String description) {
        // newsId проверяется в findNews
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException("Заголовок новости не может быть пустым при обновлении.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new BadRequestException("Описание новости не может быть пустым при обновлении.");
        }

        News news = findNews(newsId);
        news.setTitle(title);
        news.setDescription(description);
        return newsRepository.save(news);
    }

    public Page<News> findAllNews(Pageable pageable) {
        if (pageable == null) {
            // Или использовать Pageable.unpaged() / PageRequest.of(0, defaultSize)
            throw new BadRequestException("Pageable не может быть null.");
        }
        return newsRepository.findAll(pageable);
    }
}