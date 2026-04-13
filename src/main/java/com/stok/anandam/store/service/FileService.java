package com.stok.anandam.store.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    private final String uploadDir = "uploads/memos";

    public String saveMemoPhoto(MultipartFile file) throws IOException {
        Path root = Paths.get(uploadDir);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        // Clean filename and add UUID prefix to avoid collisions
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "photo.jpg";
        
        String filename = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("\\s+", "_");
        Path filePath = root.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return "memos/" + filename;
    }
}
