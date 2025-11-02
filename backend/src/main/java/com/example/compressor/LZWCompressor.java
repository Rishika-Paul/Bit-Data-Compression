package com.example.compressor;

import java.io.*;
import java.util.*;

public class LZWCompressor {
    public byte[] compress(byte[] input) throws IOException {
        if (input == null || input.length == 0) return new byte[0];

        Map<String, Integer> dict = new HashMap<>();
        for (int i = 0; i < 256; i++) dict.put("" + (char)i, i);
        int dictSize = 256;

        String w = "";
        List<Integer> codes = new ArrayList<>();
        for (byte b : input) {
            char c = (char)(b & 0xFF);
            String wc = w + c;
            if (dict.containsKey(wc)) {
                w = wc;
            } else {
                codes.add(dict.get(w));
                dict.put(wc, dictSize++);
                w = "" + c;
            }
        }
        if (!w.equals("")) codes.add(dict.get(w));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(codes.size());
        for (int code : codes) dos.writeShort(code); // 16-bit per code for demo
        dos.flush();
        return baos.toByteArray();
    }
}