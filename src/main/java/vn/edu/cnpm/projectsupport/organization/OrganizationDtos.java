package vn.edu.cnpm.projectsupport.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class OrganizationDtos {
    private OrganizationDtos() {}

    public record GroupResponse(Long id, String code, String name, Long leaderUserId,
            String leaderName, LocalDate startDate, LocalDate endDate, String status,
            long memberCount, List<UserSummary> lecturers) {}

    public record UserSummary(Long id, String username, String email, String fullName,
            String role, String status) {}

    public record GroupRequest(@NotBlank String code, @NotBlank String name,
            Long leaderUserId, LocalDate startDate, LocalDate endDate, String status) {}

    public record CreatePersonRequest(@NotBlank @Size(max = 50) String username,
            @NotBlank @Email String email, @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(min = 8) String initialPassword) {}
}
