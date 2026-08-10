package vn.edu.cnpm.projectsupport.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.auth.dto.LoginRequest;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository; // Import đúng package UserRepository của đồng đội

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean login(LoginRequest request) {
        // Đổi thành findByUsernameIgnoreCase để dùng hàm có sẵn trong UserRepository
        User user = userRepository.findByUsernameIgnoreCase(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác"));

        boolean isPasswordValid = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!isPasswordValid) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác");
        }

        return true;
    }
}
