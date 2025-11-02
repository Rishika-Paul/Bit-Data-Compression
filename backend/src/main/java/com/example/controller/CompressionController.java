package com.example.controller;

import com.example.dto.CompressionResult;
import com.example.dto.CompareResult;
import com.example.service.CompressionService;
import com.example.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api")
public class CompressionController {
    @Autowired
    private CompressionService compressionService;

    @Autowired
    private FileStorageService storage;

    private String resolveBaseUrl(HttpHeaders headers) {
        if (headers == null) return "";
        String v = headers.getFirst("X-BACKEND-BASE-URL");
        return v == null ? "" : v;
    }

    @PostMapping("/compress")
    public ResponseEntity<CompressionResult> compressSingle(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name="algorithm", required=false, defaultValue="huffman") String algorithm,
            @RequestHeader HttpHeaders headers) {
        try {
            String base = resolveBaseUrl(headers);
            CompressionResult res = compressionService.compressSingle(file, algorithm, base);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/compress/compare")
    public ResponseEntity<CompareResult> compareBoth(@RequestParam("file") MultipartFile file,
                                                     @RequestHeader HttpHeaders headers) {
        try {
            String base = resolveBaseUrl(headers);
            CompareResult res = compressionService.compareBoth(file, base);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        try {
            Path p = storage.getFilePath(filename);
            if (p == null || !p.toFile().exists()) return ResponseEntity.notFound().build();
            Resource resource = new UrlResource(p.toUri());
            String contentDisposition = "attachment; filename=\"" + p.getFileName().toString() + "\"";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}