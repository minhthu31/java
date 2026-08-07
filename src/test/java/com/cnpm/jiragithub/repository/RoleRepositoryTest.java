package com.cnpm.jiragithub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.cnpm.jiragithub.entity.Role;

@SpringBootTest
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
public void testSaveAndFindRole() {

    Role role = roleRepository.findByName("ADMIN").orElse(null);

    if (role == null) {
        role = new Role();
        role.setName("ADMIN");
        roleRepository.save(role);
    }

    Role result = roleRepository.findByName("ADMIN").orElse(null);

    assertTrue(result != null);
    assertEquals("ADMIN", result.getName());
}
}