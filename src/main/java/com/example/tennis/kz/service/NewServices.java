package com.example.tennis.kz.service;

import com.example.tennis.kz.model.News;
// import com.example.tennis.kz.model.User; // User не используется в этом сервисе напрямую
// import com.example.tennis.kz.model.response.FileResponse; // Больше не нужен
import com.example.tennis.kz.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
// import org.springframework.core.io.Resource; // Заменяется на ResponseInputStream
import software.amazon.awssdk.core.ResponseInputStream; // Импортируем ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse; // Импортируем GetObjectResponse
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
// import java.util.List; // Не используется
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NewServices {

    private final NewsRepository newsRepository;
    // private final FileService fileService; // Заменяем FileService
    private final R2StorageService r2StorageService; // Внедряем R2StorageService

    @Transactional
    public News createNews(News news, MultipartFile file) throws IOException {
        // Валидация типа файла (оставляем или переносим в контроллер/R2StorageService)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Ошибка: Поддерживаются только файлы изображений (image/*). Получен тип: " + contentType);
        }

        String imageKey = r2StorageService.uploadFile(file);

        // Сохраняем ключ R2 и Content-Type в объекте News
        news.setImage(imageKey);
        news.setImageContentType(contentType); // Сохраняем тип контента

        return newsRepository.save(news);
    }

    @Transactional // Добавляем аннотацию, т.к. операция составная (DB + R2)
    public void deleteNews(Long id) throws IOException { // IOException может быть выброшено R2 сервисом
        News news = findNews(id);
        String imageKey = news.getImage();

        // Удаляем файл из R2, если он существует
        if (imageKey != null && !imageKey.isEmpty()) {
            r2StorageService.deleteFile(imageKey);
        }

        // Удаляем новость из базы данных
        newsRepository.delete(news);
    }

    public News findNews(Long id) {
        return newsRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("News with id '%d' not found", id)));
    }

    /**
     * Загружает файл изображения новости из R2.
     * Возвращает ResponseInputStream, содержащий поток данных и метаданные объекта.
     * Контроллер должен обработать этот объект для формирования HTTP-ответа.
     *
     * @param newsId ID новости
     * @return ResponseInputStream<GetObjectResponse> или выбрасывает исключение, если изображение не найдено.
     */
    public ResponseInputStream<GetObjectResponse> downloadNewsImage(Long newsId) {
        News news = findNews(newsId);
        String imageKey = news.getImage();

        if (imageKey == null || imageKey.isEmpty()) {
            throw new NoSuchElementException(String.format("News with id '%d' has no associated image.", newsId));
        }

        // Загружаем файл из R2 по ключу
        ResponseInputStream<GetObjectResponse> responseStream = r2StorageService.downloadFile(imageKey);

        if (responseStream == null) {
            // Это может произойти, если файл был удален из R2 вручную,
            // или возникла ошибка при скачивании в r2StorageService (там д.б. логирование)
            throw new RuntimeException(String.format("Could not download image with key '%s' for news id '%d'", imageKey, newsId));
        }

        return responseStream;
    }

    // Метод обновления не затрагивает файл, оставляем как есть
    @Transactional // Добавляем на всякий случай, т.к. идет сохранение
    public News updateNews(Long newsId, String title, String description) {
        News news = findNews(newsId);
        news.setTitle(title);
        news.setDescription(description);
        return newsRepository.save(news);
    }

    public Page<News> findAllNews(Pageable pageable) {
        return newsRepository.findAll(pageable);
    }

}