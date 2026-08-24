package vn.edu.cnpm.projectsupport.security;

public interface IntegrationSecretService {
    String decrypt(String encryptedSecret);
    String encrypt(String plainSecret);
}
