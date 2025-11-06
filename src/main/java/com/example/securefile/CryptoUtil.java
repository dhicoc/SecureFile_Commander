package com.example.securefile;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;

public class CryptoUtil {
    private static final int GCM_TAG = 128;
    private static final int IV_SIZE = 12;

    public static void encryptFile(File in, File out, String keyHex) throws Exception {
        byte[] key = hexToBytes(keyHex);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[IV_SIZE];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(iv);
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                 FileInputStream fis = new FileInputStream(in)) {
                byte[] buffer = new byte[4096];
                int r;
                while ((r = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, r);
                }
            }
        }
    }

    public static void decryptFile(File in, File out, String keyHex) throws Exception {
        byte[] key = hexToBytes(keyHex);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        try (FileInputStream fis = new FileInputStream(in)) {
            byte[] iv = new byte[IV_SIZE];
            if (fis.read(iv) != iv.length) throw new IOException("Invalid file");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                 FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buffer = new byte[4096];
                int r;
                while ((r = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, r);
                }
            }
        }
    }

    private static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        int l = hex.length();
        byte[] b = new byte[l/2];
        for (int i = 0; i < b.length; i++) {
            int idx = i*2;
            b[i] = (byte) Integer.parseInt(hex.substring(idx, idx+2), 16);
        }
        return b;
    }
}