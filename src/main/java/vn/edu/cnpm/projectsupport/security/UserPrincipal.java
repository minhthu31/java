package vn.edu.cnpm.projectsupport.security;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    private String id;
    private String username;
    private String email;
}