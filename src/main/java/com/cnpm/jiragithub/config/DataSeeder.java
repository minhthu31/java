package com.cnpm.jiragithub.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cnpm.jiragithub.entity.Role;
import com.cnpm.jiragithub.repository.RoleRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {

        createRole("ADMIN");
        createRole("LECTURER");
        createRole("TEAM_LEADER");
        createRole("TEAM_MEMBER");

    }

    private void createRole(String name) {

        if (roleRepository.findByName(name).isEmpty()) {

            Role role = new Role();
            role.setName(name);

            roleRepository.save(role);

        }

    }

}