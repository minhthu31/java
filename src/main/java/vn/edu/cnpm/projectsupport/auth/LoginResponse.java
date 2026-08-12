package vn.edu.cnpm.projectsupport.auth;

public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    private String email;
    private String fullName;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String username, String email, String fullName) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }

    public LoginResponse(String username, String email, String fullName, String role) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public LoginResponse(String accessToken, String username, String email, String fullName, String role) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.username = username;
        this.email = email;
        this.fullName = fullName;
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
}
