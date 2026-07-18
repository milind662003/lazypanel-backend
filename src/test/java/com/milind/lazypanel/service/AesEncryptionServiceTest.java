package com.milind.lazypanel.service;

import com.milind.lazypanel.service.implementations.AesEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class AesEncryptionServiceTest {
    private AesEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        String base64Key = Base64.getEncoder().encodeToString(key);

        encryptionService = new AesEncryptionService(base64Key);
    }

    @Test
    void shouldEncryptAndDecryptSuccessfully() {

        String original = "sample text";

        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(original, decrypted);
    }

    @Test
    void shouldNotReturnPlainTextAfterEncryption() {

        String original = "sample text";

        String encrypted = encryptionService.encrypt(original);

        assertNotEquals(original, encrypted);
    }

    @Test
    void shouldGenerateDifferentCipherTextForSamePlainText() {

        String original = "sample text";

        String encrypted1 = encryptionService.encrypt(original);
        String encrypted2 = encryptionService.encrypt(original);

        assertNotEquals(encrypted1, encrypted2);
    }
}
