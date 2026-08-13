package vn.edu.cnpm.projectsupport.auth;

public class LoginResponse {

    private String username;
    private String email;
    private String fullName;
    private String role;
    private String accessToken;
    private String tokenType;
    private Long expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(
            String username,
            String email,
            String fullName,
            String role,
            String accessToken,
            String tokenType,
            Long expiresIn) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
}