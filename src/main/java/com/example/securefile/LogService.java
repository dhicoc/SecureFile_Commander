package com.example.securefile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class LogService {
    private static Path logFile;

    public static void init(String path) {
        try {
            logFile = Path.of(path);
            if (!Files.exists(logFile.getParent())) Files.createDirectories(logFile.getParent());
            if (!Files.exists(logFile)) Files.createFile(logFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to init log: " + e.getMessage(), e);
        }
    }

    public static synchronized void log(String type, String message) {
        String line = Instant.now().toString() + " [" + type + "] " + message;
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(logFile, java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND))) {
            pw.println(line);
        } catch (IOException e) {
            System.err.println("Log failed: " + e.getMessage());
        }
    }
}