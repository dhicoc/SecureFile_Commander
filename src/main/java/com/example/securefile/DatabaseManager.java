package com.example.securefile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager for MySQL usage.
 * Reads connection info from ConfigManager (db.url, db.user, db.password).
 *
 * This implementation returns a fresh Connection on each getConnection() call.
 * Callers should use try-with-resources to close the Connection.
 * For production, consider replacing with a connection pool (HikariCP).
 */
public class DatabaseManager {
    private static String url;
    private static String user;
    private static String pass;

    /**
     * Initialize DB config and ensure tables exist.
     * This method acquires a temporary connection to create tables and then closes it.
     */
    public static void init(String unusedPath) {
        try {
            ConfigManager cfg = ConfigManager.loadDefault();
            url = cfg.get("db.url");
            user = cfg.get("db.user");
            pass = cfg.get("db.password");
            if (url == null || user == null) {
                throw new RuntimeException("Please configure db.url and db.user in config/config.properties");
            }
            // create tables using a temporary connection
            try (Connection temp = DriverManager.getConnection(url, user, pass)) {
                ensureTables(temp);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    private static void ensureTables(Connection conn) throws SQLException {
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

    /**
     * Returns a new Connection. Caller is responsible for closing it.
     */
    public static Connection getConnection() throws SQLException {
        if (url == null || user == null) {
            throw new IllegalStateException("DatabaseManager not initialized. Call DatabaseManager.init(...) first.");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * No-op for this implementation.
     */
    public static void close() {
        // Nothing to close since we return fresh connections per call.
    }
}