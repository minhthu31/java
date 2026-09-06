package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLinkId;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskLinkMatchedFrom;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskLinkSource;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLink;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueRepository;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class GitHubTaskLinkServiceTest {

    @Mock JiraIssueRepository jiraIssueRepository;
    @Mock TaskRepository taskRepository;
    @Mock TaskCommitLinkRepository commitLinkRepository;
    @Mock TaskPullRequestLinkRepository pullRequestLinkRepository;

    private GitHubTaskLinkService service;

    @BeforeEach
    void setUp() {
        service = new GitHubTaskLinkService(
                jiraIssueRepository, taskRepository, commitLinkRepository, pullRequestLinkRepository);
    }

    @Test
    void extractsUniqueValidKeysWithoutMatchingMalformedText() {
        assertThat(GitHubTaskLinkService.extractIssueKeys(
                "feature/CNPM-97-link CNPM-97 and TEST_2-18; invalid CNPM-0 1CNPM-12 CNPM-2abc"))
                .containsExactly("CNPM-97", "TEST_2-18");
    }

    @Test
    void linksCommitKeysAndKeepsMissingKeysAsWarnings() {
        GitHubCommit commit = mock(GitHubCommit.class);
        when(commit.getId()).thenReturn(31L);
        when(commit.getMessage()).thenReturn("feat(CNPM-97): link activity; refs CNPM-404");
        stubTask("CNPM-97", 7L, 1L);
        when(jiraIssueRepository.findByJiraIssueKey("CNPM-404")).thenReturn(Optional.empty());

        GitHubTaskLinkResult result = service.linkCommit(1L, commit);

        assertThat(result.keysDetected()).isEqualTo(2);
        assertThat(result.linksCreated()).isEqualTo(1);
        assertThat(result.warnings()).containsExactly("Jira Issue Key không tồn tại: CNPM-404");
        ArgumentCaptor<TaskCommitLink> captor = ArgumentCaptor.forClass(TaskCommitLink.class);
        verify(commitLinkRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(new TaskCommitLinkId(7L, 31L));
        assertThat(captor.getValue().getLinkSource()).isEqualTo(TaskLinkSource.AUTO);
        assertThat(captor.getValue().getMatchedFrom()).isEqualTo(TaskLinkMatchedFrom.COMMIT_MESSAGE);
    }

    @Test
    void linksMultiplePullRequestKeysUsingFirstMatchedSource() {
        GitHubPullRequest pullRequest = mock(GitHubPullRequest.class);
        when(pullRequest.getId()).thenReturn(44L);
        when(pullRequest.getHeadRef()).thenReturn("feature/CNPM-97-links");
        when(pullRequest.getTitle()).thenReturn("CNPM-98 expose activity API");
        when(pullRequest.getBody()).thenReturn("Also mentions CNPM-97 again");
        stubTask("CNPM-97", 7L, 1L);
        stubTask("CNPM-98", 8L, 1L);

        GitHubTaskLinkResult result = service.linkPullRequest(1L, pullRequest);

        assertThat(result.keysDetected()).isEqualTo(2);
        assertThat(result.linksCreated()).isEqualTo(2);
        ArgumentCaptor<TaskPullRequestLink> captor = ArgumentCaptor.forClass(TaskPullRequestLink.class);
        verify(pullRequestLinkRepository, times(2)).saveAndFlush(captor.capture());
        List<TaskPullRequestLink> links = captor.getAllValues();
        assertThat(links.get(0).getMatchedFrom()).isEqualTo(TaskLinkMatchedFrom.BRANCH);
        assertThat(links.get(1).getMatchedFrom()).isEqualTo(TaskLinkMatchedFrom.PR_TITLE);
    }

    @Test
    void rejectsIssueFromAnotherProjectWithoutCreatingLink() {
        GitHubCommit commit = mock(GitHubCommit.class);
        when(commit.getId()).thenReturn(31L);
        when(commit.getMessage()).thenReturn("CNPM-97");
        stubTask("CNPM-97", 7L, 2L);

        GitHubTaskLinkResult result = service.linkCommit(1L, commit);

        assertThat(result.linksCreated()).isZero();
        assertThat(result.warnings())
                .containsExactly("Jira Issue Key không thuộc project hiện tại: CNPM-97");
        verifyNoInteractions(commitLinkRepository);
    }

    @Test
    void rerunDoesNotCreateDuplicateCommitLink() {
        GitHubCommit commit = mock(GitHubCommit.class);
        when(commit.getId()).thenReturn(31L);
        when(commit.getMessage()).thenReturn("CNPM-97");
        stubTask("CNPM-97", 7L, 1L);
        when(commitLinkRepository.existsById(new TaskCommitLinkId(7L, 31L))).thenReturn(true);

        GitHubTaskLinkResult result = service.linkCommit(1L, commit);

        assertThat(result.linksCreated()).isZero();
        assertThat(result.duplicateLinks()).isEqualTo(1);
        verify(commitLinkRepository, never()).saveAndFlush(any());
    }

    private void stubTask(String issueKey, Long taskId, Long projectId) {
        JiraIssue issue = mock(JiraIssue.class);
        when(issue.getTaskId()).thenReturn(taskId);
        when(jiraIssueRepository.findByJiraIssueKey(issueKey)).thenReturn(Optional.of(issue));
        Task task = mock(Task.class);
        lenient().when(task.getId()).thenReturn(taskId);
        when(task.getProjectId()).thenReturn(projectId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    }
}
