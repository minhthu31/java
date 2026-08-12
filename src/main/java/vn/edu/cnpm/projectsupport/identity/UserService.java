package vn.edu.cnpm.projectsupport.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.domain.UserStatus;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Mã hóa mật khẩu thô
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Tạo tài khoản mới: Mã hóa mật khẩu trước khi lưu vào CSDL
     */
    @Transactional
    public User registerUser(String username, String email, String rawPassword, String fullName) {
        String passwordHash = encodePassword(rawPassword);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash); 
        user.setFullName(fullName);
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }
}
