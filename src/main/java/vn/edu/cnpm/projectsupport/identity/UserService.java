package vn.edu.cnpm.projectsupport.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.identity.domain.User; // Import đúng User từ package domain
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository; // Import đúng UserRepository từ package repository

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String username, String email, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(encodedPassword)
                .status("ACTIVE")
                .build();

        return userRepository.save(user);
    }
}
