package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
@Transactional(readOnly = true)
public class GitHubActivityService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final GitHubCommitRepository commitRepository;
    private final GitHubPullRequestRepository pullRequestRepository;
    private final TaskCommitLinkRepository commitLinkRepository;
    private final TaskPullRequestLinkRepository pullRequestLinkRepository;
    private final UserExternalAccountRepository userExternalAccountRepository;

    public GitHubActivityService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            GitHubRepositoryRepository repositoryRepository,
            GitHubCommitRepository commitRepository,
            GitHubPullRequestRepository pullRequestRepository,
            TaskCommitLinkRepository commitLinkRepository,
            TaskPullRequestLinkRepository pullRequestLinkRepository,
            UserExternalAccountRepository userExternalAccountRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.commitLinkRepository = commitLinkRepository;
        this.pullRequestLinkRepository = pullRequestLinkRepository;
        this.userExternalAccountRepository = userExternalAccountRepository;
    }

    public PageResponse<CommitResponse> listCommits(Long projectId, Long repositoryId, String issueKey, Pageable pageable) {
        validateProjectAndRepository(projectId, repositoryId);
        Page<GitHubCommit> commits = (issueKey == null || issueKey.isBlank())
                ? commitRepository.findByRepositoryId(repositoryId, pageable)
                : commitRepository.findByRepositoryIdAndExactIssueKey(repositoryId, issueKey.trim(), pageable);
        return PageResponse.from(commits.map(this::mapToCommitResponse));
    }

    public PageResponse<PullRequestResponse> listPullRequests(
            Long projectId, Long repositoryId, String state, String issueKey, Pageable pageable) {
        validateProjectAndRepository(projectId, repositoryId);
        GitHubPullRequestState parsedState = parseState(state);

        Page<GitHubPullRequest> prs = (issueKey == null || issueKey.isBlank())
                ? pullRequestRepository.findByRepositoryIdAndState(repositoryId, parsedState, pageable)
                : pullRequestRepository.findByRepositoryIdAndStateAndExactIssueKey(repositoryId, parsedState, issueKey.trim(), pageable);
        return PageResponse.from(prs.map(this::mapToPullRequestResponse));
    }

    public PageResponse<GitHubActivityResponse> listActivities(
            Long projectId, Long actorUserId, String type, String issueKey, Instant from, Instant to, Pageable pageable) {
        validateProject(projectId);
        validateFilters(from, to, type);

        String normalizedType = (type == null || type.isBlank()) ? "COMMIT" : type.trim().toUpperCase();
        String normalizedKey = (issueKey != null && !issueKey.isBlank()) ? issueKey.trim() : null;

        if ("PULL_REQUEST".equals(normalizedType)) {
            Page<GitHubPullRequest> prs = (normalizedKey == null)
                    ? pullRequestRepository.findUnifiedActivityWithoutIssueKey(projectId, actorUserId, null, from, to, pageable)
                    : pullRequestRepository.findUnifiedActivityWithIssueKey(projectId, actorUserId, null, normalizedKey, from, to, pageable);
            return PageResponse.from(prs.map(this::mapPrToActivity));
        }

        Page<GitHubCommit> commits = (normalizedKey == null)
                ? commitRepository.findUnifiedActivityWithoutIssueKey(projectId, actorUserId, from, to, pageable)
                : commitRepository.findUnifiedActivityWithIssueKey(projectId, actorUserId, normalizedKey, from, to, pageable);
        return PageResponse.from(commits.map(this::mapCommitToActivity));
    }

    public PageResponse<GitHubActivityResponse> listTaskActivities(Long projectId, Long taskId, Pageable pageable) {
        validateProject(projectId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task không tồn tại: " + taskId));
        if (!projectId.equals(task.getProjectId())) {
            throw new IllegalArgumentException("Task không thuộc project: " + projectId);
        }

        Page<GitHubCommit> commits = commitRepository.findByTaskIdPaged(taskId, pageable);
        if (!commits.isEmpty()) {
            return PageResponse.from(commits.map(this::mapCommitToActivity));
        }
        Page<GitHubPullRequest> prs = pullRequestRepository.findByTaskIdPaged(taskId, pageable);
        return PageResponse.from(prs.map(this::mapPrToActivity));
    }

    private void validateFilters(Instant from, Instant to, String type) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ: from phải <= to");
        }
        if (type != null && !type.isBlank()) {
            String upper = type.trim().toUpperCase();
            if (!upper.equals("COMMIT") && !upper.equals("PULL_REQUEST")) {
                throw new IllegalArgumentException("type chỉ nhận COMMIT hoặc PULL_REQUEST");
            }
        }
    }

    private GitHubPullRequestState parseState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        try {
            return GitHubPullRequestState.valueOf(state.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái Pull Request không hợp lệ: " + state);
        }
    }

    private void validateProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NoSuchElementException("Project không tồn tại: " + projectId);
        }
    }

    private void validateProjectAndRepository(Long projectId, Long repositoryId) {
        validateProject(projectId);
        GitHubRepository repo = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new NoSuchElementException("Repository không tồn tại: " + repositoryId));
        if (!projectId.equals(repo.getProjectId())) {
            throw new IllegalArgumentException("Repository không thuộc project: " + projectId);
        }
    }

    private Long resolveLocalUserId(Long authorExternalAccountId) {
        if (authorExternalAccountId == null) {
            return null;
        }
        return userExternalAccountRepository.findById(authorExternalAccountId)
                .map(UserExternalAccount::getUserId)
                .orElse(null);
    }

    private CommitResponse mapToCommitResponse(GitHubCommit commit) {
        return new CommitResponse(
                commit.getId(),
                commit.getRepositoryId(),
                commit.getSha(),
                commit.getMessage(),
                commit.getAuthorGithubUserId(),
                commit.getAuthorLogin(),
                commit.getCommittedAt(),
                commit.getHtmlUrl(),
                commit.getAdditions(),
                commit.getDeletions(),
                commit.getFilesChanged(),
                new ArrayList<>(GitHubTaskLinkService.extractIssueKeys(commit.getMessage()))
        );
    }

    private PullRequestResponse mapToPullRequestResponse(GitHubPullRequest pr) {
        List<String> keys = new ArrayList<>(GitHubTaskLinkService.extractIssueKeys(pr.getHeadRef() + " " + pr.getTitle() + " " + (pr.getBody() != null ? pr.getBody() : "")));
        return new PullRequestResponse(
                pr.getId(),
                pr.getRepositoryId(),
                pr.getGithubPullRequestId(),
                pr.getNumber(),
                pr.getTitle(),
                pr.getAuthorGithubUserId(),
                pr.getAuthorLogin(),
                pr.getHeadRef(),
                pr.getHeadSha(),
                pr.getBaseRef(),
                pr.getState(),
                pr.isDraft(),
                pr.getMergedAt(),
                pr.getHtmlUrl(),
                keys
        );
    }

    private GitHubActivityResponse mapCommitToActivity(GitHubCommit commit) {
        List<Long> linkedTaskIds = commitLinkRepository.findByIdCommitId(commit.getId())
                .stream().map(link -> link.getId().getTaskId()).toList();
        return new GitHubActivityResponse(
                "COMMIT",
                commit.getSha(),
                commit.getMessage(),
                resolveLocalUserId(commit.getAuthorExternalAccountId()),
                commit.getAuthorLogin(),
                commit.getCommittedAt(),
                commit.getHtmlUrl(),
                new ArrayList<>(GitHubTaskLinkService.extractIssueKeys(commit.getMessage())),
                linkedTaskIds
        );
    }

    private GitHubActivityResponse mapPrToActivity(GitHubPullRequest pr) {
        List<Long> linkedTaskIds = pullRequestLinkRepository.findByIdPullRequestId(pr.getId())
                .stream().map(link -> link.getId().getTaskId()).toList();
        List<String> keys = new ArrayList<>(GitHubTaskLinkService.extractIssueKeys(pr.getHeadRef() + " " + pr.getTitle() + " " + (pr.getBody() != null ? pr.getBody() : "")));
        return new GitHubActivityResponse(
                "PULL_REQUEST",
                String.valueOf(pr.getNumber()),
                pr.getTitle(),
                resolveLocalUserId(pr.getAuthorExternalAccountId()),
                pr.getAuthorLogin(),
                pr.getCreatedAt(),
                pr.getHtmlUrl(),
                keys,
                linkedTaskIds
        );
    }
}