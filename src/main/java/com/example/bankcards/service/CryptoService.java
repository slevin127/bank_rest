package com.example.bankcards.service;

import com.example.bankcards.config.AppProperties;
import com.example.bankcards.exception.CryptoException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final AppProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public CryptoService(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String key = properties.getCrypto().getSecretKey();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("Crypto secret key must be 16, 24, or 32 bytes long");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public EncryptedData encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return new EncryptedData(
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv));
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Failed to encrypt card number", e);
        }
    }

    /**
     * Decrypts the provided encrypted data using AES-GCM encryption.
     *
     * @param data the encrypted data containing the ciphertext and the initialization vector
     * @return the decrypted plaintext as a string
     * @throws CryptoException if decryption fails due to invalid data or a cryptographic error
     */
     String decrypt(EncryptedData data) {
        try {
            byte[] iv = Base64.getDecoder().decode(data.initializationVector());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data.cipherText()));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Failed to decrypt card number", e);
        }
    }
}
