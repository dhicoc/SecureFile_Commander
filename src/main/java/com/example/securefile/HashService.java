package com.example.securefile;

import java.io.FileInputStream;
import java.security.MessageDigest;

public class HashService {
    public static String sha256Hex(String filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = fis.read(buf)) != -1) md.update(buf, 0, r);
        }
        byte[] d = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte by : d) sb.append(String.format("%02x", by));
        return sb.toString();
    }
}