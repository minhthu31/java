package vn.edu.cnpm.projectsupport.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.auth.dto.LoginRequest;
import vn.edu.cnpm.projectsupport.identity.User;
import vn.edu.cnpm.projectsupport.identity.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác"));

        // [AC 3] Kiểm tra mật khẩu bằng passwordEncoder.matches()
        boolean isPasswordValid = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!isPasswordValid) {
            // [BR-10 / FR-01] Không thông báo chi tiết lỗi để tránh rò rỉ an ninh
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác");
        }

        return true;
    }
}
