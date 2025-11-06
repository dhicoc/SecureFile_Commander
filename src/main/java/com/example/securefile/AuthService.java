package com.example.securefile;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;

public class AuthService {

    public AuthService() {
    }

    public void ensureAdmin() {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM accounts WHERE username = ?")) {
            ps.setString(1, "admin");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    boolean ok = false;
                    try {
                        ok = registerInternal("admin", "admin123", "admin");
                    } catch (SQLException e) {
                        System.err.println("ERROR: failed to create default admin:");
                        e.printStackTrace();
                    }
                    System.out.println("Created default admin/admin123? " + ok + ". Please change password after login.");
                } else {
                    System.out.println("ensureAdmin: admin user already exists or could not determine count.");
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: ensureAdmin encountered SQLException:");
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.err.println("ERROR: ensureAdmin unexpected exception:");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean register(String username, String password) {
        try {
            return registerInternal(username, password, "user");
        } catch (Exception e) {
            System.err.println("ERROR: register failed for username=" + username);
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerInternal(String username, String password, String role) throws SQLException {
        // 生成 bcrypt 哈希
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        // 仅打印哈希前缀用于调试（不要打印完整哈希或明文密码到生产日志）
        String hashPrefix = (hash != null ? hash.substring(0, Math.min(16, hash.length())) : "null");
        System.out.println("DEBUG: registerInternal username=" + username + " hashPrefix=" + hashPrefix + " role=" + role);

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO accounts(username,password_hash,role) VALUES(?,?,?)")) {
            // 打印当前连接的 JDBC URL（用于确认连接的目标数据库）
            try {
                DatabaseMetaData md = c.getMetaData();
                if (md != null) {
                    System.out.println("DEBUG: registerInternal DB URL = " + md.getURL());
                }
            } catch (Exception mdEx) {
                System.out.println("DEBUG: unable to get DB metadata: " + mdEx.getMessage());
            }

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, role);
            int rows = ps.executeUpdate();
            System.out.println("DEBUG: registerInternal executeUpdate returned rows=" + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("ERROR: registerInternal SQLException for username=" + username);
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("ERROR: registerInternal unexpected exception for username=" + username);
            e.printStackTrace();
            throw new SQLException("Unexpected exception in registerInternal", e);
        }
    }

    public Account login(String username, String password) {
        if (username == null) {
            System.out.println("DEBUG: login called with null username");
            return null;
        }
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, password_hash, role FROM accounts WHERE username = ?")) {

            // 打印当前连接的 JDBC URL（帮助确认连接目标）
            try {
                DatabaseMetaData md = c.getMetaData();
                if (md != null) {
                    System.out.println("DEBUG: login DB URL = " + md.getURL());
                }
            } catch (Exception mdEx) {
                System.out.println("DEBUG: unable to get DB metadata: " + mdEx.getMessage());
            }

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("DEBUG: login - no user row for username=" + username);
                    return null;
                }

                int id = rs.getInt("id");
                String hash = rs.getString("password_hash");
                String role = rs.getString("role");

                // 打印一些非敏感调试信息
                String hashPrefix = (hash != null ? hash.substring(0, Math.min(16, hash.length())) : "null");
                System.out.println("DEBUG: login attempt username=" + username + " dbId=" + id + " dbRole=" + role + " dbHashPrefix=" + hashPrefix);

                if (hash == null || hash.isEmpty()) {
                    System.out.println("DEBUG: login - password_hash is null/empty for username=" + username);
                    return null;
                }

                try {
                    BCrypt.Result res = BCrypt.verifyer().verify(password.toCharArray(), hash);
                    System.out.println("DEBUG: bcrypt verified=" + res.verified + " for username=" + username);
                    if (res.verified) {
                        return new Account(id, username, role);
                    } else {
                        System.out.println("DEBUG: login - password verification failed for username=" + username);
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: bcrypt verification threw exception for username=" + username);
                    e.printStackTrace();
                    // 继续返回 null，表明登录失败
                }
            }
        } catch (SQLException e) {
            System.err.println("ERROR: login SQLException for username=" + username);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR: login unexpected exception for username=" + username);
            e.printStackTrace();
        }
        return null;
    }

    public boolean changePassword(String username, String newPassword) {
        String hash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        String hashPrefix = (hash != null ? hash.substring(0, Math.min(16, hash.length())) : "null");
        System.out.println("DEBUG: changePassword username=" + username + " newHashPrefix=" + hashPrefix);
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE accounts SET password_hash = ? WHERE username = ?")) {

            try {
                DatabaseMetaData md = c.getMetaData();
                if (md != null) {
                    System.out.println("DEBUG: changePassword DB URL = " + md.getURL());
                }
            } catch (Exception mdEx) {
                System.out.println("DEBUG: unable to get DB metadata: " + mdEx.getMessage());
            }

            ps.setString(1, hash);
            ps.setString(2, username);
            int updated = ps.executeUpdate();
            System.out.println("DEBUG: changePassword updatedRows=" + updated);
            return updated > 0;
        } catch (SQLException e) {
            System.err.println("ERROR: changePassword SQLException for username=" + username);
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("ERROR: changePassword unexpected exception for username=" + username);
            e.printStackTrace();
            return false;
        }
    }
}