package com.example.securefile;

public class EncryptTask extends AbstractTask {
    private final String src;
    private final String dest;

    public EncryptTask(String src, String dest, String owner) {
        super("Encrypt " + src + " -> " + dest, owner);
        this.src = src;
        this.dest = dest;
    }

    @Override
    public void run() {
        try {
            ConfigManager cfg = ConfigManager.loadDefault();
            FileService.encryptFile(src, dest, cfg.getAesKey());
            String h = HashService.sha256Hex(dest);
            LogService.log("ENCRYPT_TASK", name + " owner=" + owner + " hash=" + h);
        } catch (Exception e) {
            LogService.log("ENCRYPT_TASK_ERROR", e.getMessage());
        }
    }
}