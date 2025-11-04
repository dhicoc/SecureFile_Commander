package com.example.securefile;

import java.util.Scanner;

public class Console {
    private final Scanner scanner = new Scanner(System.in);
    private final AuthService authService;
    private final TaskManager taskManager;
    private final ConfigManager configManager;
    private Account currentUser = null;

    public Console() {
        configManager = ConfigManager.loadDefault();
        DatabaseManager.init(configManager.get("db.url")); // param ignored in MySQL version
        authService = new AuthService();
        taskManager = new TaskManager();
        LogService.init(configManager.getLogPath());
    }

    public void start() {
        System.out.println("Welcome to SecureFile Commander Pro (Console)");
        System.out.println("Type 'help' for commands.");
        authService.ensureAdmin();
        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();
            try {
                switch (cmd) {
                    case "help": printHelp(); break;
                    case "register": handleRegister(); break;
                    case "login": handleLogin(); break;
                    case "logout": handleLogout(); break;
                    case "whoami": handleWhoami(); break;
                    case "submit": handleSubmit(parts); break;
                    case "tasks": handleTasks(); break;
                    case "cancel": handleCancel(parts); break;
                    case "backup": handleBackup(parts); break;
                    case "encrypt": handleEncrypt(parts); break;
                    case "restore": handleRestore(parts); break;
                    case "hash": handleHash(parts); break;
                    case "config": handleConfig(parts); break;
                    case "exit":
                    case "quit": shutdown(); return;
                    default:
                        System.out.println("Unknown command. Type 'help'.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                LogService.log("ERROR", ex.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  register            - Create new user");
        System.out.println("  login               - Login");
        System.out.println("  logout              - Logout");
        System.out.println("  whoami              - Show current user");
        System.out.println("  submit backup <src> <dest>   - Submit backup task");
        System.out.println("  submit encrypt <src> <dest>  - Submit encrypt task");
        System.out.println("  tasks               - List tasks");
        System.out.println("  cancel <taskId>     - Cancel task");
        System.out.println("  backup <src> <dest> - Run backup synchronously");
        System.out.println("  encrypt <src> <dest> - Run encrypt synchronously");
        System.out.println("  restore <backupPath> <dest> - Restore from backup");
        System.out.println("  hash <file>         - Compute SHA-256 hash");
        System.out.println("  config set <k> <v>  - Set config");
        System.out.println("  config get <k>      - Get config");
        System.out.println("  exit                - Exit program");
    }

    private void handleRegister() {
        System.out.print("username: ");
        String username = scanner.nextLine().trim();
        System.out.print("password: ");
        String password = scanner.nextLine().trim();
        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("username/password cannot be empty");
            return;
        }
        boolean ok = authService.register(username, password);
        System.out.println(ok ? "Registered." : "Registration failed (exists?).");
    }

    private void handleLogin() {
        if (currentUser != null) {
            System.out.println("Already logged in as " + currentUser.getUsername());
            return;
        }
        System.out.print("username: ");
        String username = scanner.nextLine().trim();
        System.out.print("password: ");
        String password = scanner.nextLine().trim();
        Account acc = authService.login(username, password);
        if (acc != null) {
            currentUser = acc;
            System.out.println("Login successful. Welcome " + acc.getUsername());
            LogService.log("LOGIN", acc.getUsername());
        } else {
            System.out.println("Login failed.");
        }
    }

    private void handleLogout() {
        if (currentUser == null) {
            System.out.println("Not logged in.");
            return;
        }
        System.out.println("User " + currentUser.getUsername() + " logged out.");
        LogService.log("LOGOUT", currentUser.getUsername());
        currentUser = null;
    }

    private void handleWhoami() {
        if (currentUser == null) System.out.println("Not logged in.");
        else System.out.println("Current user: " + currentUser.getUsername());
    }

    private void handleSubmit(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: submit <backup|encrypt> ...");
            return;
        }
        String type = parts[1];
        if ("backup".equals(type)) {
            if (parts.length < 4) { System.out.println("Usage: submit backup <src> <dest>"); return; }
            String src = parts[2], dest = parts[3];
            BackupTask task = new BackupTask(src, dest, currentUser==null ? "anonymous" : currentUser.getUsername());
            int id = taskManager.submitTask(task);
            System.out.println("Submitted backup task id=" + id);
        } else if ("encrypt".equals(type)) {
            if (parts.length < 4) { System.out.println("Usage: submit encrypt <src> <dest>"); return; }
            String src = parts[2], dest = parts[3];
            EncryptTask task = new EncryptTask(src, dest, currentUser==null ? "anonymous" : currentUser.getUsername());
            int id = taskManager.submitTask(task);
            System.out.println("Submitted encrypt task id=" + id);
        } else {
            System.out.println("Unknown submit type.");
        }
    }

    private void handleTasks() {
        taskManager.printTasks();
    }

    private void handleCancel(String[] parts) {
        if (parts.length < 2) { System.out.println("Usage: cancel <taskId>"); return; }
        try {
            int id = Integer.parseInt(parts[1]);
            boolean ok = taskManager.cancelTask(id);
            System.out.println(ok ? "Cancelled." : "Not found or cannot cancel.");
        } catch (NumberFormatException ex) {
            System.out.println("Invalid id.");
        }
    }

    private void handleBackup(String[] parts) throws Exception {
        if (parts.length < 3) { System.out.println("Usage: backup <src> <dest>"); return; }
        String src = parts[1], dest = parts[2];
        FileService.backup(src, dest);
        LogService.log("BACKUP_SYNC", src + "->" + dest);
        System.out.println("Backup completed.");
    }

    private void handleEncrypt(String[] parts) throws Exception {
        if (parts.length < 3) { System.out.println("Usage: encrypt <src> <dest>"); return; }
        String src = parts[1], dest = parts[2];
        FileService.encryptFile(src, dest, configManager.getAesKey());
        LogService.log("ENCRYPT_SYNC", src + "->" + dest);
        System.out.println("Encrypt completed.");
    }

    private void handleRestore(String[] parts) throws Exception {
        if (parts.length < 3) { System.out.println("Usage: restore <backupPath> <dest>"); return; }
        String backup = parts[1], dest = parts[2];
        FileService.restore(backup, dest);
        LogService.log("RESTORE", backup + "->" + dest);
        System.out.println("Restore completed.");
    }

    private void handleHash(String[] parts) throws Exception {
        if (parts.length < 2) { System.out.println("Usage: hash <file>"); return; }
        String f = parts[1];
        String hash = HashService.sha256Hex(f);
        System.out.println("SHA-256: " + hash);
    }

    private void handleConfig(String[] parts) {
        if (parts.length < 2) { System.out.println("Usage: config <get|set> ..."); return; }
        String op = parts[1];
        if ("get".equals(op)) {
            if (parts.length < 3) { System.out.println("Usage: config get <key>"); return; }
            String v = configManager.get(parts[2]);
            System.out.println(parts[2] + "=" + v);
        } else if ("set".equals(op)) {
            if (parts.length < 4) { System.out.println("Usage: config set <key> <value>"); return; }
            configManager.set(parts[2], parts[3]);
            configManager.save();
            System.out.println("Saved.");
        } else {
            System.out.println("Unknown config operation.");
        }
    }

    private void shutdown() {
        System.out.println("Shutting down...");
        taskManager.shutdown();
        DatabaseManager.close();
    }
}