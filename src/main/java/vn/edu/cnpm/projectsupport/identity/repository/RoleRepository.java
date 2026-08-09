package vn.edu.cnpm.projectsupport.identity.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleCode code);
    Optional<Role> findByName(String name);
}