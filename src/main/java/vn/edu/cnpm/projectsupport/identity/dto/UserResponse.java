package vn.edu.cnpm.projectsupport.identity.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String status;

    // [AC 4] Không khai báo password / passwordHash ở DTO này
}
