package com.example.compressor;

import java.io.*;
import java.util.*;

public class HuffmanCompressor {
    private static class Node implements Comparable<Node> {
        byte b;
        int freq;
        Node left, right;
        Node(byte b, int freq) { this.b = b; this.freq = freq; }
        Node(Node l, Node r) { this.left = l; this.right = r; this.freq = l.freq + r.freq; }
        public boolean isLeaf() { return left == null && right == null; }
        @Override public int compareTo(Node o) { return Integer.compare(this.freq, o.freq); }
    }

    public byte[] compress(byte[] input) throws IOException {
        if (input == null || input.length == 0) return new byte[0];

        int[] freq = new int[256];
        for (byte b : input) freq[b & 0xFF]++;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) pq.add(new Node((byte)i, freq[i]));
        }
        if (pq.size() == 0) return new byte[0];
        if (pq.size() == 1) pq.add(new Node((byte)0, 0));

        while (pq.size() > 1) {
            Node a = pq.poll(), b = pq.poll();
            pq.add(new Node(a, b));
        }
        Node root = pq.poll();

        Map<Byte, String> codes = new HashMap<>();
        buildCodes(root, "", codes);

        StringBuilder sb = new StringBuilder();
        for (byte b : input) sb.append(codes.get(b));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // write frequency table (256 ints) so decoding is possible
        for (int i = 0; i < 256; i++) dos.writeInt(freq[i]);
        int len = sb.length();
        dos.writeInt(len);
        int cur = 0, bitCount = 0;
        for (int i = 0; i < len; i++) {
            cur = (cur << 1) | (sb.charAt(i) - '0');
            bitCount++;
            if (bitCount == 8) {
                dos.writeByte(cur);
                cur = 0; bitCount = 0;
            }
        }
        if (bitCount > 0) {
            cur <<= (8 - bitCount);
            dos.writeByte(cur);
        }
        dos.flush();
        return baos.toByteArray();
    }

    private void buildCodes(Node node, String s, Map<Byte, String> map) {
        if (node.isLeaf()) {
            map.put(node.b, s.length() > 0 ? s : "0");
            return;
        }
        buildCodes(node.left, s + '0', map);
        buildCodes(node.right, s + '1', map);
    }
}