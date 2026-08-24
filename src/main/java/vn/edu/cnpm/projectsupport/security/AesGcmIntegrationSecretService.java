package vn.edu.cnpm.projectsupport.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AesGcmIntegrationSecretService implements IntegrationSecretService {
    private static final String PREFIX = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmIntegrationSecretService(
            @Value("${app.security.integration-encryption-key}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.length() < 32) {
            throw new IllegalArgumentException("INTEGRATION_ENCRYPTION_KEY must contain at least 32 characters");
        }
        this.key = encryptionKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String encrypt(String plainSecret) {
        if (plainSecret == null || plainSecret.isBlank()) {
            throw new IllegalArgumentException("Secret must not be blank");
        }
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plainSecret.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt integration secret", exception);
        }
    }

    @Override
    public String decrypt(String encryptedSecret) {
        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            throw new IllegalArgumentException("Encrypted secret must not be blank");
        }
        if (!encryptedSecret.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Integration secret is not encrypted with the supported format");
        }
        String[] parts = encryptedSecret.split(":", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted integration secret");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unable to decrypt integration secret", exception);
        }
    }

    private SecretKeySpec keySpec() {
        byte[] normalized = java.util.Arrays.copyOf(key, 32);
        return new SecretKeySpec(normalized, "AES");
    }
}
