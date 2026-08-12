package vn.edu.cnpm.projectsupport.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.common.exception.InvalidCredentialsException;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.domain.UserStatus;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

        String roleName = (user.getRole() != null) ? user.getRole().getName() : null;

        
        String token = jwtTokenProvider.generateToken(user.getUsername(), roleName);

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roleName
        );
    }
}
