package com.example.tennis.kz.service;

import com.example.tennis.kz.model.News;
import com.example.tennis.kz.model.User;
import com.example.tennis.kz.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NewServices {

    private final NewsRepository newsRepository;
    private final FileService fileService;

    public News createNews(News news, MultipartFile file) throws IOException {
        news.setImage(fileService.saveFile(file));
        return newsRepository.save(news);
    }

    public void deleteNews(Long id) throws IOException {
        News news = findNews(id);
        fileService.deleteFile(news.getImage());
        newsRepository.delete(news);
    }

    public News findNews(Long id) {
        return newsRepository.findById(id).orElseThrow(()
                -> new NoSuchElementException(String.format("News with id '%d' not found", id)));
    }

    public Resource downloadNews(Long newsId) {
        News news = findNews(newsId);
        return fileService.loadFileAsResource(news.getImage());
    }

    public News updateNews(Long newsId, String title, String description) {
        News news = findNews(newsId);
        news.setTitle(title);
        news.setDescription(description);
        return newsRepository.save(news);
    }

    public List<News> findAllNews() {
        return newsRepository.findAll();
    }

}
