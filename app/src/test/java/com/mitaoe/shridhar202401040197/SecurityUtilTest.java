package com.mitaoe.shridhar202401040197;

import com.mitaoe.shridhar202401040197.util.SecurityUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SecurityUtilTest {

    @Test
    public void testGenerateSalt() {
        String salt1 = SecurityUtil.generateSalt();
        String salt2 = SecurityUtil.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals("Generated salts should be unique", salt1, salt2);
        assertEquals("16 bytes in hex should have 32 characters", 32, salt1.length());
    }

    @Test
    public void testHashPasswordDeterministicForSameSalt() {
        String password = "SecretPassword123!";
        String salt = SecurityUtil.generateSalt();

        String hash1 = SecurityUtil.hashPassword(password, salt);
        String hash2 = SecurityUtil.hashPassword(password, salt);

        assertEquals("Same password and salt must produce same hash", hash1, hash2);
        assertNotEquals("Hash should not equal raw password", password, hash1);
    }

    @Test
    public void testHashPasswordDiffersWithDifferentSalt() {
        String password = "SecretPassword123!";
        String salt1 = SecurityUtil.generateSalt();
        String salt2 = SecurityUtil.generateSalt();

        String hash1 = SecurityUtil.hashPassword(password, salt1);
        String hash2 = SecurityUtil.hashPassword(password, salt2);

        assertNotEquals("Different salts must produce different hashes", hash1, hash2);
    }

    @Test
    public void testVerifyPassword() {
        String password = "CorrectPassword42";
        String wrongPassword = "WrongPassword";
        String salt = SecurityUtil.generateSalt();
        String hash = SecurityUtil.hashPassword(password, salt);

        assertTrue("Verification should succeed for correct password",
                SecurityUtil.verifyPassword(password, hash, salt));
        assertFalse("Verification should fail for incorrect password",
                SecurityUtil.verifyPassword(wrongPassword, hash, salt));
        assertFalse("Verification should fail for null input",
                SecurityUtil.verifyPassword(null, hash, salt));
    }
}
