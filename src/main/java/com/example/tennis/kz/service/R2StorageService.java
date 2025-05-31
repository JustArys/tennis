package com.example.tennis.kz.service;

import com.example.tennis.kz.exception.BadRequestException; // Импорт
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException; // Импорт
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл для загрузки не может быть пустым.");
        }
        // Проверка типа контента, если она должна быть здесь, а не в вызывающем сервисе:
        // String contentType = file.getContentType();
        // if (contentType == null || !contentType.startsWith("image/")) { // Пример для изображений
        //     throw new BadRequestException("Поддерживаются только файлы изображений. Получен тип: " + contentType);
        // }


        String key = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize()));
            return key;
        } catch (S3Exception e) {
            // Ошибки S3 (например, проблемы с доступом, конфигурацией) указывают на серверную проблему.
            System.err.println("S3 Error uploading file '" + key + "': " + e.awsErrorDetails().errorMessage());
            // Преобразуем в RuntimeException, что приведет к HTTP 500 через GlobalExceptionHandler
            throw new RuntimeException("Ошибка при загрузке файла в хранилище: " + e.getMessage(), e);
        } catch (IOException e) {
            // Ошибки чтения из MultipartFile
            System.err.println("IO Error processing upload for file '" + (file.getOriginalFilename()) + "': " + e.getMessage());
            throw e; // Перебрасываем IOException, GlobalExceptionHandler может его обработать как 500
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new BadRequestException("Ключ файла для скачивания не может быть пустым.");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            // Файл не найден в R2 - это 404
            System.err.println("File not found in R2: " + key);
            throw new NoSuchElementException("Файл не найден в хранилище по ключу: " + key, e);
        } catch (S3Exception e) {
            // Другие ошибки S3 при скачивании - серверная проблема
            System.err.println("S3 Error downloading file '" + key + "': " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Ошибка при скачивании файла из хранилища: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new BadRequestException("Ключ файла для удаления не может быть пустым.");
        }
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            // S3 deleteObject обычно не выбрасывает ошибку, если ключ не существует (идемпотентность).
            // Если файл не найден, операция просто ничего не делает на стороне S3.
        } catch (S3Exception e) {
            // Ошибки S3 при удалении (например, проблемы с правами) - серверная проблема
            System.err.println("S3 Error deleting file '" + key + "': " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Ошибка при удалении файла из хранилища: " + e.getMessage(), e);
        }
    }
}