package com.example.dto;

public class CompressionResult {
    private long originalSize;
    private long compressedSize;
    private double compressionRatio;
    private long timeMs;
    private String downloadUrl;

    public CompressionResult() {}

    public CompressionResult(long originalSize, long compressedSize, long timeMs, String downloadUrl) {
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.timeMs = timeMs;
        this.compressionRatio = originalSize == 0 ? 0.0 : ((double) compressedSize) / originalSize;
        this.downloadUrl = downloadUrl;
    }

    // getters + setters
    public long getOriginalSize() { return originalSize; }
    public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }
    public long getCompressedSize() { return compressedSize; }
    public void setCompressedSize(long compressedSize) { this.compressedSize = compressedSize; }
    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double compressionRatio) { this.compressionRatio = compressionRatio; }
    public long getTimeMs() { return timeMs; }
    public void setTimeMs(long timeMs) { this.timeMs = timeMs; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}