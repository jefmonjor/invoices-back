package com.invoices.shared.infrastructure.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionServiceTest {

    private AesEncryptionService encryptionService;
    private static final String TEST_KEY = Base64.getEncoder()
            .encodeToString("12345678901234567890123456789012".getBytes()); // 32 bytes

    @BeforeEach
    void setUp() {
        encryptionService = new AesEncryptionService(TEST_KEY);
    }

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String originalText = "Sensitive Data 123";

        String encrypted = encryptionService.encrypt(originalText);
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void shouldProduceDifferentCiphertextForSamePlaintext() {
        String originalText = "Same Data";

        String encrypted1 = encryptionService.encrypt(originalText);
        String encrypted2 = encryptionService.encrypt(originalText);

        assertNotNull(encrypted1);
        assertNotNull(encrypted2);
        assertNotEquals(encrypted1, encrypted2, "Ciphertext should be different due to random IV");

        assertEquals(originalText, encryptionService.decrypt(encrypted1));
        assertEquals(originalText, encryptionService.decrypt(encrypted2));
    }

    @Test
    void shouldHandleNulls() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void shouldReturnOriginalTextIfDecryptionFails_SoftFailStrategy() {
        String plainText = "NotEncryptedData";
        // This is not a valid Base64 string or encrypted data
        String result = encryptionService.decrypt(plainText);

        // According to our Soft Fail strategy, it should return the input if it fails
        assertEquals(plainText, result);
    }
}
