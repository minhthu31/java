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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsernameIgnoreCase(request.getUsernameOrEmail())
                .or(() -> userRepository
                        .findByEmailIgnoreCase(request.getUsernameOrEmail()))
                .orElseThrow(() ->
                        new RuntimeException("Invalid username/email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException(
                    "Username/email hoặc password không đúng");
        }

        // Tiêu chí 3: Kiểm tra mật khẩu bcrypt khi đăng nhập
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new InvalidCredentialsException(
                    "Username/email hoặc password không đúng");
        }

        // Tiêu chí 4: Chỉ trả về thông tin user, KHÔNG chứa password hay passwordHash
        return new LoginResponse(
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
        );
    }
}
