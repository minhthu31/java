package vn.edu.cnpm.projectsupport.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.repository.ReportingRepository;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock SprintRepository sprintRepository;
    @Mock ReportingRepository reportingRepository;
    @Mock CurrentUserService currentUserService;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(projectRepository, sprintRepository, reportingRepository, currentUserService);
    }

    @Test
    void calculatesDistinctGithubCountsAndUsesExclusiveTo() {
        Long projectId = 1L;
        Long memberId = 7L;
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-08T00:00:00Z");
        Instant lastSync = Instant.parse("2026-09-08T00:30:00Z");

        User admin = mockUser(99L, RoleCode.ADMIN);
        ProjectRepository.ActiveMemberProjection member = mock(ProjectRepository.ActiveMemberProjection.class);
        when(member.getId()).thenReturn(memberId);
        when(member.getUsername()).thenReturn("member.test");
        when(member.getFullName()).thenReturn("Test Member");

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectRepository.findActiveMembers(projectId)).thenReturn(List.of(member));
        when(currentUserService.findCurrentUser()).thenReturn(Optional.of(admin));
        var statusCounts = List.of(status("DONE", 2L), status("TO_DO", 1L));
        when(reportingRepository.countTasksByStatus(
                eq(projectId), isNull(), eq(memberId), eq(from), eq(to), any(Instant.class)))
                .thenReturn(statusCounts);
        when(reportingRepository.countOverdueTasks(
                eq(projectId), isNull(), eq(memberId), eq(from), eq(to), any(Instant.class))).thenReturn(1L);
        when(reportingRepository.isGithubLinked(memberId)).thenReturn(true);
        when(reportingRepository.countCommits(
                eq(projectId), isNull(), eq(memberId), eq(from), eq(to), any(Instant.class))).thenReturn(4L);
        when(reportingRepository.countPullRequests(
                eq(projectId), isNull(), eq(memberId), eq(from), eq(to), any(Instant.class))).thenReturn(3L);
        when(reportingRepository.countPullRequestsByState(
                eq(projectId), isNull(), eq(memberId), eq("OPEN"), eq(from), eq(to), any(Instant.class))).thenReturn(1L);
        when(reportingRepository.countPullRequestsByState(
                eq(projectId), isNull(), eq(memberId), eq("CLOSED"), eq(from), eq(to), any(Instant.class))).thenReturn(1L);
        when(reportingRepository.countPullRequestsByState(
                eq(projectId), isNull(), eq(memberId), eq("MERGED"), eq(from), eq(to), any(Instant.class))).thenReturn(1L);
        when(reportingRepository.countLinkedTasks(
                eq(projectId), isNull(), eq(memberId), eq(from), eq(to), any(Instant.class))).thenReturn(2L);
        when(reportingRepository.findLastSuccessfulGithubSync(projectId)).thenReturn(lastSync);

        var response = service.getProjectSummary(
                projectId, new ReportFilterRequest(null, memberId, from, to));

        assertThat(response.memberContributions()).singleElement().satisfies(c -> {
            assertThat(c.githubLinked()).isTrue();
            assertThat(c.commits()).isEqualTo(4);
            assertThat(c.pullRequests()).isEqualTo(3);
            assertThat(c.openPullRequests()).isEqualTo(1);
            assertThat(c.closedPullRequests()).isEqualTo(1);
            assertThat(c.mergedPullRequests()).isEqualTo(1);
            assertThat(c.linkedTasks()).isEqualTo(2);
        });

        verify(reportingRepository).countPullRequests(
                eq(projectId), isNull(), eq(memberId), eq(from), eq(to), any(Instant.class));
        verify(reportingRepository).countCommits(
                eq(projectId), isNull(), eq(memberId), eq(from), eq(to), any(Instant.class));
        assertThat(response.to()).isEqualTo(to);
    }

    @Test
    void rejectsFromEqualToTo() {
        Instant point = Instant.parse("2026-09-08T00:00:00Z");
        when(projectRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.getProjectSummary(1L, new ReportFilterRequest(null, null, point, point)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must be earlier than to");

        verifyNoInteractions(currentUserService);
    }

    @Test
    void teamMemberCanOnlySeeOwnStatistics() {
        Long projectId = 1L;
        Long currentId = 7L;
        User memberUser = mockUser(currentId, RoleCode.TEAM_MEMBER);
        ProjectRepository.ActiveMemberProjection member = mock(ProjectRepository.ActiveMemberProjection.class);
        when(member.getId()).thenReturn(currentId);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectRepository.findActiveMembers(projectId)).thenReturn(List.of(member));
        when(currentUserService.findCurrentUser()).thenReturn(Optional.of(memberUser));

        assertThatThrownBy(() -> service.getProjectSummary(projectId, new ReportFilterRequest(null, 8L, null, null)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verifyNoInteractions(reportingRepository);
    }

    @Test
    void unlinkedGithubAccountIsExplicit() {
        Long projectId = 1L;
        Long memberId = 7L;
        User admin = mock(User.class);
        Role adminRole = mock(Role.class);
        when(admin.getRole()).thenReturn(adminRole);
        when(adminRole.getCode()).thenReturn(RoleCode.ADMIN);
        ProjectRepository.ActiveMemberProjection member = mock(ProjectRepository.ActiveMemberProjection.class);
        when(member.getId()).thenReturn(memberId);
        when(member.getUsername()).thenReturn("member.test");
        when(member.getFullName()).thenReturn("Test Member");

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectRepository.findActiveMembers(projectId)).thenReturn(List.of(member));
        when(currentUserService.findCurrentUser()).thenReturn(Optional.of(admin));
        when(reportingRepository.countTasksByStatus(anyLong(), isNull(), eq(memberId), isNull(), isNull(), any(Instant.class)))
                .thenReturn(List.of());
        when(reportingRepository.countOverdueTasks(anyLong(), isNull(), eq(memberId), isNull(), isNull(), any(Instant.class)))
                .thenReturn(0L);
        when(reportingRepository.isGithubLinked(memberId)).thenReturn(false);
        when(reportingRepository.countCommits(anyLong(), isNull(), eq(memberId), isNull(), isNull(), any(Instant.class))).thenReturn(0L);
        when(reportingRepository.countPullRequests(anyLong(), isNull(), eq(memberId), isNull(), isNull(), any(Instant.class))).thenReturn(0L);
        when(reportingRepository.countPullRequestsByState(anyLong(), isNull(), eq(memberId), anyString(), isNull(), isNull(), any(Instant.class))).thenReturn(0L);
        when(reportingRepository.countLinkedTasks(anyLong(), isNull(), eq(memberId), isNull(), isNull(), any(Instant.class))).thenReturn(0L);
        when(reportingRepository.findLastSuccessfulGithubSync(projectId)).thenReturn(null);

        var response = service.getProjectSummary(projectId, new ReportFilterRequest(null, memberId, null, null));

        assertThat(response.memberContributions()).singleElement().satisfies(c -> {
            assertThat(c.githubLinked()).isFalse();
            assertThat(c.commits()).isZero();
            assertThat(c.pullRequests()).isZero();
        });
        assertThat(response.warnings()).contains("GITHUB_ACCOUNT_NOT_LINKED");
    }

    private User mockUser(Long id, RoleCode roleCode) {
        User user = mock(User.class);
        Role role = mock(Role.class);
        if (roleCode == RoleCode.TEAM_MEMBER) {
            when(user.getId()).thenReturn(id);
        }
        when(user.getRole()).thenReturn(role);
        when(role.getCode()).thenReturn(roleCode);
        return user;
    }

    private ReportingRepository.StatusCountProjection status(String status, long count) {
        ReportingRepository.StatusCountProjection projection = mock(ReportingRepository.StatusCountProjection.class);
        when(projection.getStatus()).thenReturn(status);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }
}
