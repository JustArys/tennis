package com.example.tennis.kz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class R2StorageService {


    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file) throws IOException {
        // Генерируем уникальное имя файла (или используем оригинальное, если нужно)
        String key = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType()) // Устанавливаем Content-Type
                // Можно добавить другие метаданные при необходимости
                // .metadata(Map.of("original-filename", file.getOriginalFilename()))
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            // Используем RequestBody.fromInputStream для потоковой загрузки
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize()));
            return key; // Возвращаем ключ (имя) загруженного файла
        } catch (S3Exception e) {
            // Обработка ошибок S3 (например, проблемы с доступом)
            System.err.println("S3 Error uploading file: " + e.awsErrorDetails().errorMessage());
            throw new IOException("Failed to upload file to R2", e);
        } catch (IOException e) {
            // Обработка ошибок ввода-вывода
            System.err.println("IO Error uploading file: " + e.getMessage());
            throw e;
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Возвращает InputStream для чтения файла
            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            System.err.println("File not found in R2: " + key);
            // Здесь можно выбросить свое кастомное исключение или вернуть null/Optional.empty()
            return null;
        } catch (S3Exception e) {
            System.err.println("S3 Error downloading file: " + e.awsErrorDetails().errorMessage());
            // Обработка других ошибок S3
            return null;
        }
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            System.err.println("S3 Error deleting file: " + e.awsErrorDetails().errorMessage());
            // Обработка ошибок
        }
    }
}
