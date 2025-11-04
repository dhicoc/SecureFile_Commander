package com.example.securefile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager for MySQL usage.
 * Reads connection info from ConfigManager (db.url, db.user, db.password).
 */
public class DatabaseManager {
    private static Connection conn;

    public static void init(String unusedPath) {
        try {
            ConfigManager cfg = ConfigManager.loadDefault();
            String url = cfg.get("db.url");
            String user = cfg.get("db.user");
            String pass = cfg.get("db.password");
            if (url == null || user == null) {
                throw new RuntimeException("Please configure db.url and db.user in config/config.properties");
            }
            conn = DriverManager.getConnection(url, user, pass);
            ensureTables();
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    private static void ensureTables() throws SQLException {
        String createUsers = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(255) UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "role VARCHAR(50) NOT NULL DEFAULT 'user'" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.execute(createUsers);
        }
    }

    public static Connection getConnection() {
        return conn;
    }

    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            // ignore
        }
    }
}