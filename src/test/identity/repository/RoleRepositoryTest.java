package vn.edu.cnpm.projectsupport.identity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;

@DataJpaTest
@ActiveProfiles("test")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldSaveAndReadRoleByName() {
        Role role = new Role(RoleCode.ADMIN, "Quản trị viên");

        Role savedRole = roleRepository.save(role);

        Optional<Role> foundRole =
                roleRepository.findByName("Quản trị viên");

        assertThat(savedRole.getId()).isNotNull();
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getCode()).isEqualTo(RoleCode.ADMIN);
        assertThat(foundRole.get().getName()).isEqualTo("Quản trị viên");
    }
}