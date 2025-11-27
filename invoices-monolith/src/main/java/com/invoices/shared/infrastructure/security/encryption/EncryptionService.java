package com.invoices.shared.infrastructure.security.encryption;

/**
 * Service for encrypting and decrypting data.
 */
public interface EncryptionService {

    /**
     * Encrypts the given plain text.
     * 
     * @param plainText The text to encrypt.
     * @return The encrypted text (usually Base64 encoded).
     */
    String encrypt(String plainText);

    /**
     * Decrypts the given encrypted text.
     * 
     * @param encryptedText The encrypted text (Base64 encoded).
     * @return The decrypted plain text.
     */
    String decrypt(String encryptedText);
}
