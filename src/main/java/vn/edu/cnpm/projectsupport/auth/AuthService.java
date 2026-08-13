package vn.edu.cnpm.projectsupport.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.common.exception.InvalidCredentialsException;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.domain.UserStatus;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String identifier = request.getUsernameOrEmail();

        User user = userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new InvalidCredentialsException("Username/email hoặc password không đúng"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Username/email hoặc password không đúng");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Username/email hoặc password không đúng");
        }

        // Dùng getCode() lấy mã Role chuẩn (ADMIN, LECTURER, TEAM_LEADER, TEAM_MEMBER)
        String roleCode = null;
        if (user.getRole() != null && user.getRole().getCode() != null) {
            roleCode = user.getRole().getCode().name();
        }

        // Chưa trả token vì JWT属于 task CNPM-42 chưa sẵn sàng trên main
        String token = null;

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roleCode
        );
    }
}