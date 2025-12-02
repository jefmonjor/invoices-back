package com.invoices.shared.domain.ports;

/**
 * Port for encryption/decryption operations.
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of specific encryption implementation.
 */
public interface EncryptionService {

    /**
     * Encrypts the given plain text.
     *
     * @param plaintext the text to encrypt
     * @return encrypted string
     * @throws Exception if encryption fails
     */
    String encrypt(String plaintext) throws Exception;

    /**
     * Decrypts the given encrypted text.
     *
     * @param encryptedText the text to decrypt
     * @return decrypted string
     * @throws Exception if decryption fails
     */
    String decrypt(String encryptedText) throws Exception;
}
