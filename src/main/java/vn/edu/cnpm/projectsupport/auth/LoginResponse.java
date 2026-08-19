package vn.edu.cnpm.projectsupport.auth;

public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String username;
    private String email;
    private String fullName;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            String username,
            String email,
            String fullName,
            String role) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }

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

    public String getRole() { return role; }
}
