package vn.edu.cnpm.projectsupport.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.domain.UserStatus;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByUsernameIgnoreCase(username)
                .or(() -> userRepository.findByEmailIgnoreCase(username))
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().getCode().name())
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }
}