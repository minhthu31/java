package vn.edu.cnpm.projectsupport.security;
import static org.assertj.core.api.Assertions.assertThat; import org.junit.jupiter.api.Test;
class JwtTokenProviderTests {@Test void generatedTokenContainsIdentityAndRole(){var provider=new JwtTokenProvider("test-only-jwt-secret-key-with-at-least-32-bytes",3600000);var claims=provider.parseClaims(provider.generateToken("leader","TEAM_LEADER"));assertThat(claims.getSubject()).isEqualTo("leader");assertThat(claims.get("role")).isEqualTo("TEAM_LEADER");}}
