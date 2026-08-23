package vn.edu.cnpm.projectsupport.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.exception.InvalidCredentialsException;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.domain.UserStatus;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProjectRepository projectRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmailIgnoreCase(request.getUsernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Tên đăng nhập/email hoặc mật khẩu không đúng"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Tài khoản đã bị vô hiệu hóa");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Tên đăng nhập/email hoặc mật khẩu không đúng");
        }
        String role = user.getRole().getCode().name();
        Long projectId = user.getId() == null
                ? null
                : projectRepository.findFirstAccessibleProjectId(user.getId()).orElse(null);
        return new LoginResponse(jwtTokenProvider.generateToken(user.getUsername(), role), "Bearer",
                jwtTokenProvider.getExpirationSeconds(), user.getUsername(), user.getEmail(),
                user.getFullName(), role, user.getId(), projectId);
    }
}
