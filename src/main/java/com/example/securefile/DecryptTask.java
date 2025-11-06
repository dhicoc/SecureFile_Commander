package com.example.securefile;

public class DecryptTask extends AbstractTask {
    private final String src;
    private final String dest;

    public DecryptTask(String src, String dest, String owner) {
        super("Decrypt " + src + " -> " + dest, owner);
        this.src = src;
        this.dest = dest;
    }

    @Override
    public void run() {
        try {
            ConfigManager cfg = ConfigManager.loadDefault();
            FileService.decryptFile(src, dest, cfg.getAesKey());
            String h = HashService.sha256Hex(dest);
            LogService.log("DECRYPT_TASK", name + " owner=" + owner + " hash=" + h);
        } catch (Exception e) {
            LogService.log("DECRYPT_TASK_ERROR", e.getMessage());
        }
    }
}
