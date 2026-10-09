package com.mitaoe.shridhar202401040197.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SecurityUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        if (password == null) password = "";
        if (salt == null) salt = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes());
            byte[] hash = digest.digest(password.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback
            return Integer.toHexString((password + salt).hashCode());
        }
    }

    public static boolean verifyPassword(String enteredPassword, String storedHash, String storedSalt) {
        if (storedHash == null || enteredPassword == null) return false;
        String calculated = hashPassword(enteredPassword, storedSalt);
        return MessageDigest.isEqual(calculated.getBytes(), storedHash.getBytes());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
