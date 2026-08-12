package vn.edu.cnpm.projectsupport.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/admin/test")
    public String admin() {
        return "ADMIN";
    }

    @GetMapping("/api/lecturer/test")
    public String lecturer() {
        return "LECTURER";
    }

    @GetMapping("/api/team-leader/test")
    public String teamLeader() {
        return "TEAM_LEADER";
    }

    @GetMapping("/api/member/test")
    public String member() {
        return "TEAM_MEMBER";
    }
}