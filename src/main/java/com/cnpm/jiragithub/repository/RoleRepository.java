package com.cnpm.jiragithub.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cnpm.jiragithub.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

}