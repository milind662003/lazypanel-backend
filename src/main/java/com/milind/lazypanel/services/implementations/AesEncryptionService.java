package com.milind.lazypanel.services.implementations;

import com.milind.lazypanel.exception.EncryptionException;
import com.milind.lazypanel.services.interfaces.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

@Service
public class AesEncryptionService implements EncryptionService {

    private final SecretKey secretKey;
    private static final int TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    public AesEncryptionService(@Value("${aes.secretKey}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }
    @Override
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] ivBytes = generateIv();
            GCMParameterSpec iv = new GCMParameterSpec(TAG_LENGTH, ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] ivPrependedCipherText = new byte[ivBytes.length+cipherText.length];
            System.arraycopy(ivBytes, 0, ivPrependedCipherText, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, ivPrependedCipherText, IV_LENGTH, cipherText.length);
            return Base64.getEncoder()
                    .encodeToString(ivPrependedCipherText);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] ivPrependedCipherText = Base64.getDecoder().decode(cipherText);
            byte[] ivBytes = Arrays.copyOfRange(ivPrependedCipherText, 0, IV_LENGTH);
            GCMParameterSpec iv = new GCMParameterSpec(TAG_LENGTH, ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] plainText = cipher.doFinal(Arrays.copyOfRange(ivPrependedCipherText, IV_LENGTH, ivPrependedCipherText.length));
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    public static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
