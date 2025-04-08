package com.example.tennis.kz.service;


import com.example.tennis.kz.model.response.FileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {
    @Value("${file.upload.directory}")
    private String uploadDirectory;

    public FileResponse saveFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        // --- Валидация: Проверяем, что это изображение ---
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Ошибка: Поддерживаются только файлы изображений (image/*). Получен тип: " + contentType);
        }
        // --- Конец валидации ---

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename()); // Очищаем имя файла
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        }

        // Генерируем уникальное имя файла, чтобы избежать коллизий и проблем с именами
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(uniqueFileName).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Возвращаем результат с уникальным именем файла и типом контента
        return new FileResponse(uniqueFileName, contentType);
    }


    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                return null;
            }
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public void renameFile(String oldFileName, String newFileName) throws IOException {
        Path oldFilePath = Paths.get(uploadDirectory, oldFileName);
        Path newFilePath = Paths.get(uploadDirectory, newFileName);
        Files.move(oldFilePath, newFilePath);
    }

    public void deleteFile(String fileName) throws IOException {
        Path filePath = Paths.get(uploadDirectory, fileName);
        Files.delete(filePath);
    }
}
