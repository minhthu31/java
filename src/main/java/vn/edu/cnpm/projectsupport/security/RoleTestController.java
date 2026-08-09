package vn.edu.cnpm.projectsupport.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleTestController {

    @GetMapping("/api/admin/test")
    public ResponseEntity<String> admin() {
        return ResponseEntity.ok("ADMIN access granted");
    }

    @GetMapping("/api/lecturer/test")
    public ResponseEntity<String> lecturer() {
        return ResponseEntity.ok("LECTURER access granted");
    }

    @GetMapping("/api/team-leader/test")
    public ResponseEntity<String> teamLeader() {
        return ResponseEntity.ok("TEAM_LEADER access granted");
    }

    @GetMapping("/api/member/test")
    public ResponseEntity<String> member() {
        return ResponseEntity.ok("TEAM_MEMBER access granted");
    }
}