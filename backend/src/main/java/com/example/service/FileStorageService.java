package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path tmpDir;

    public FileStorageService() throws IOException {
        tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "compression-service");
        Files.createDirectories(tmpDir);
    }

    public Path saveTemp(MultipartFile file) throws IOException {
        String fname = UUID.randomUUID().toString() + "-" + sanitize(file.getOriginalFilename());
        Path dest = tmpDir.resolve(fname);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return dest;
    }

    public Path saveBytesTemp(byte[] data, String suffix) throws IOException {
        String fname = UUID.randomUUID().toString() + suffix;
        Path dest = tmpDir.resolve(fname);
        Files.write(dest, data);
        return dest;
    }

    public Path getFilePath(String filename) {
        return tmpDir.resolve(filename);
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}