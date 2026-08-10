package vn.edu.cnpm.projectsupport.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Tiêu chí 2: Hàm mã hóa password trước khi lưu vào CSDL
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
