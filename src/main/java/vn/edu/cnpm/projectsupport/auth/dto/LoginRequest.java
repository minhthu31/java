package vn.edu.cnpm.projectsupport.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LoginRequest {

    private String username;

    /**
     * [AC 4] Loại bỏ password khỏi hàm toString() để không ghi vào Log file
     */
    @ToString.Exclude
    private String password;
}
