package com.milind.lazypanel.service.interfaces;

public interface EncryptionService {

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
