package com.cnpm.jiragithub.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.cnpm.jiragithub.entity.Role;
import com.cnpm.jiragithub.entity.User;

@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void testSaveAndFindUser() {

        Role role = roleRepository.findByName("TEAM_MEMBER").orElse(null);

        if (role == null) {
        role = new Role();
        role.setName("TEAM_MEMBER");
        roleRepository.save(role);
}

        User user = new User();

        String username = "user" + System.currentTimeMillis();

        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setPassword("123456");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setRole(role);

        userRepository.save(user);

        Optional<User> result = userRepository.findByUsername(username);

        assertTrue(result.isPresent());
        assertEquals(username + "@gmail.com", result.get().getEmail());
    }
}