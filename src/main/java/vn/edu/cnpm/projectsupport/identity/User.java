package vn.edu.cnpm.projectsupport.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore // [AC 4] Bỏ qua trường này khi Serialize sang JSON
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 20)
    private String status;
}
