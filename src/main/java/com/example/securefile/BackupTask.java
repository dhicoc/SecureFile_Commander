package com.example.securefile;

public class BackupTask extends AbstractTask {
    private final String src;
    private final String dest;

    public BackupTask(String src, String dest, String owner) {
        super("Backup " + src + " -> " + dest, owner);
        this.src = src;
        this.dest = dest;
    }

    @Override
    public void run() {
        try {
            FileService.backup(src, dest);
            String h = HashService.sha256Hex(dest);
            LogService.log("BACKUP_TASK", name + " owner=" + owner + " hash=" + h);
        } catch (Exception e) {
            LogService.log("BACKUP_TASK_ERROR", e.getMessage());
        }
    }
}