package com.example.dto;

public class CompareResult {
    private CompressionResult huffman;
    private CompressionResult lzw;
    public CompressionResult getHuffman() { return huffman; }
    public void setHuffman(CompressionResult huffman) { this.huffman = huffman; }
    public CompressionResult getLzw() { return lzw; }
    public void setLzw(CompressionResult lzw) { this.lzw = lzw; }
}