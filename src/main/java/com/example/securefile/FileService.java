package com.example.securefile;

import java.io.*;
import java.nio.file.*;

public class FileService {

    public static void backup(String srcPath, String destPath) throws IOException {
        Path src = Path.of(srcPath);
        Path dest = Path.of(destPath);
        if (!Files.exists(src)) throw new FileNotFoundException("Source not found: " + srcPath);
        if (Files.isDirectory(src)) {
            Files.walk(src).forEach(p -> {
                try {
                    Path rel = src.relativize(p);
                    Path target = dest.resolve(rel);
                    if (Files.isDirectory(p)) {
                        if (!Files.exists(target)) Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } else {
            if (dest.getParent() != null) Files.createDirectories(dest.getParent());
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void restore(String backupPath, String destPath) throws IOException {
        backup(backupPath, destPath);
    }

    public static void encryptFile(String srcPath, String destPath, String aesKeyHex) throws Exception {
        Path src = Path.of(srcPath);
        Path dest = Path.of(destPath);
        if (!Files.exists(src)) throw new FileNotFoundException("Source not found: " + srcPath);
        if (dest.getParent() != null) Files.createDirectories(dest.getParent());
        CryptoUtil.encryptFile(src.toFile(), dest.toFile(), aesKeyHex);
    }

    public static void decryptFile(String srcPath, String destPath, String aesKeyHex) throws Exception {
        CryptoUtil.decryptFile(new File(srcPath), new File(destPath), aesKeyHex);
    }
}