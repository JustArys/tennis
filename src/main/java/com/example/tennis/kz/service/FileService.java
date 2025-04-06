package com.example.tennis.kz.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {
    @Value("${file.upload.directory}")
    private String uploadDirectory;

    public String saveFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileName = originalFileName;
        int counter = 1;

        Path filePath = Paths.get(uploadDirectory, fileName);
        while (Files.exists(filePath)) {
            String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
            String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
            fileName = baseName + "_" + counter + extension;
            filePath = Paths.get(uploadDirectory, fileName);
            counter++;
        }

        Files.copy(file.getInputStream(), filePath);
        return fileName;
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
