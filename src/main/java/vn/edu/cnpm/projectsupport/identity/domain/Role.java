package vn.edu.cnpm.projectsupport.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private RoleCode code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    protected Role() {
    }

    public Role(RoleCode code, String name) {
        this.code = code;
        this.name = name;
    }

    public RoleCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
