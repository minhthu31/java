package vn.edu.cnpm.projectsupport.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Transactional
    public User createUser(Role role, String username, String email, String rawPassword, String fullName) {
        User user = new User(role, username, email, encodePassword(rawPassword), fullName);
        return userRepository.save(user);
    }
}
