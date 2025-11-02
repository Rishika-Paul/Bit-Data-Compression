package com.example.service;

import com.example.compressor.HuffmanCompressor;
import com.example.compressor.LZWCompressor;
import com.example.dto.CompressionResult;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Service
public class CompressionService {
    @Autowired
    private FileStorageService storage;

    private final HuffmanCompressor huffman = new HuffmanCompressor();
    private final LZWCompressor lzw = new LZWCompressor();

    // Extract text and images from PDF, returning concatenated bytes.
    private byte[] buildPayloadForPdf(Path pdfPath) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            // text
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            byte[] textBytes = text == null ? new byte[0] : text.getBytes();

            // images - using traditional loop instead of streams
            List<byte[]> images = new ArrayList<>();
            
            for (PDPage page : doc.getPages()) {
                Iterable<COSName> xObjectNames = page.getResources().getXObjectNames();
                
                for (COSName name : xObjectNames) {
                    try {
                        var xobj = page.getResources().getXObject(name);
                        if (xobj instanceof PDImageXObject) {
                            PDImageXObject img = (PDImageXObject) xobj;
                            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                BufferedImage bi = img.getImage();
                                // write as PNG (canonical lossless representation)
                                ImageIO.write(bi, "png", baos);
                                images.add(baos.toByteArray());
                            } catch (IOException e) {
                                // skip problematic images
                            }
                        }
                    } catch (IOException ex) {
                        // skip problematic xobjects
                    }
                }
            }

            // Concatenate: text length + text + number of images + each image length + image bytes
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                // write text length and bytes
                DataOutputStream dos = new DataOutputStream(out);
                dos.writeInt(textBytes.length);
                dos.write(textBytes);
                dos.writeInt(images.size());
                for (byte[] im : images) {
                    dos.writeInt(im.length);
                    dos.write(im);
                }
                dos.flush();
                return out.toByteArray();
            }
        }
    }

    // For standalone images, load canonical PNG bytes (so compressors operate on consistent encoding)
    private byte[] getCanonicalImageBytes(Path imagePath) throws IOException {
        try (InputStream in = Files.newInputStream(imagePath)) {
            BufferedImage bi = ImageIO.read(in);
            if (bi == null) {
                // fallback: return raw bytes
                return Files.readAllBytes(imagePath);
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(bi, "png", baos);
                return baos.toByteArray();
            }
        }
    }

    // For generic files: read raw bytes
    private byte[] readRawBytes(Path p) throws IOException {
        return java.nio.file.Files.readAllBytes(p);
    }

    // Build payload depending on file type
    public byte[] buildPayload(Path saved, String originalFilename) throws IOException {
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
            return buildPayloadForPdf(saved);
        }
        if (originalFilename != null && (originalFilename.toLowerCase().endsWith(".png")
                || originalFilename.toLowerCase().endsWith(".jpg")
                || originalFilename.toLowerCase().endsWith(".jpeg"))) {
            return getCanonicalImageBytes(saved);
        }
        // default raw bytes (txt, others)
        return readRawBytes(saved);
    }

    public CompressionResult compressSingle(MultipartFile file, String algorithm, String backendBaseUrl) throws IOException {
        Path saved = storage.saveTemp(file);
        byte[] payload = buildPayload(saved, file.getOriginalFilename());
        long originalSize = payload.length;

        Instant start = Instant.now();
        byte[] compressed;
        if ("lzw".equalsIgnoreCase(algorithm)) {
            compressed = lzw.compress(payload);
        } else {
            compressed = huffman.compress(payload);
        }
        long timeMs = Duration.between(start, Instant.now()).toMillis();

        Path out = storage.saveBytesTemp(compressed, "." + (algorithm == null ? "huffman" : algorithm) + ".bin");
        String downloadUrl = (backendBaseUrl == null ? "" : backendBaseUrl) + "/api/download/" + out.getFileName().toString();
        return new CompressionResult(originalSize, compressed.length, timeMs, downloadUrl);
    }

    public com.example.dto.CompareResult compareBoth(MultipartFile file, String backendBaseUrl) throws IOException {
        Path saved = storage.saveTemp(file);
        byte[] payload = buildPayload(saved, file.getOriginalFilename());
        long originalSize = payload.length;

        Instant s1 = Instant.now();
        byte[] h = huffman.compress(payload);
        long t1 = Duration.between(s1, Instant.now()).toMillis();
        Path out1 = storage.saveBytesTemp(h, ".huffman.bin");
        com.example.dto.CompressionResult resH =
                new com.example.dto.CompressionResult(originalSize, h.length, t1,
                        (backendBaseUrl == null ? "" : backendBaseUrl) + "/api/download/" + out1.getFileName().toString());

        Instant s2 = Instant.now();
        byte[] l = lzw.compress(payload);
        long t2 = Duration.between(s2, Instant.now()).toMillis();
        Path out2 = storage.saveBytesTemp(l, ".lzw.bin");
        com.example.dto.CompressionResult resL =
                new com.example.dto.CompressionResult(originalSize, l.length, t2,
                        (backendBaseUrl == null ? "" : backendBaseUrl) + "/api/download/" + out2.getFileName().toString());

        com.example.dto.CompareResult cr = new com.example.dto.CompareResult();
        cr.setHuffman(resH);
        cr.setLzw(resL);
        return cr;
    }
}