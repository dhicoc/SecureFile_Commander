package com.example.securefile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {
    private final Properties props = new Properties();
    private final Path file;

    private ConfigManager(Path file) {
        this.file = file;
    }

    public static ConfigManager loadDefault() {
        try {
            Path cfgDir = Path.of("config");
            if (!Files.exists(cfgDir)) Files.createDirectories(cfgDir);
            Path cfg = cfgDir.resolve("config.properties");
            ConfigManager cm = new ConfigManager(cfg);
            if (Files.exists(cfg)) {
                try (InputStream in = Files.newInputStream(cfg)) {
                    cm.props.load(in);
                }
            } else {
                cm.props.setProperty("db.url", "jdbc:mysql://localhost:3306/securefile_db?useSSL=false&serverTimezone=UTC");
                cm.props.setProperty("db.user", "root");
                cm.props.setProperty("db.password", "password");
                cm.props.setProperty("db.path", "data/securefile.db");
                cm.props.setProperty("log.path", "data/operations.log");
                cm.props.setProperty("aes.key", "0123456789abcdef0123456789abcdef");
                cm.save();
            }
            Path data = Path.of("data");
            if (!Files.exists(data)) Files.createDirectories(data);
            return cm;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage(), e);
        }
    }

    public String getDbPath() {
        return props.getProperty("db.path", "data/securefile.db");
    }

    public String getLogPath() {
        return props.getProperty("log.path", "data/operations.log");
    }

    public String getAesKey() {
        return props.getProperty("aes.key");
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    public void save() {
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "SecureFile Commander Pro Config");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}