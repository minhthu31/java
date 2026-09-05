package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
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

    public GitHubActivityService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            GitHubRepositoryRepository repositoryRepository,
            GitHubCommitRepository commitRepository,
            GitHubPullRequestRepository pullRequestRepository,
            TaskCommitLinkRepository commitLinkRepository,
            TaskPullRequestLinkRepository pullRequestLinkRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.commitLinkRepository = commitLinkRepository;
        this.pullRequestLinkRepository = pullRequestLinkRepository;
    }

    public PageResponse<CommitResponse> listCommits(Long projectId, Long repositoryId, String issueKey, Pageable pageable) {
        validateProjectAndRepository(projectId, repositoryId);
        Page<GitHubCommit> commits = commitRepository.findByRepositoryIdAndFilter(repositoryId, issueKey, pageable);
        return PageResponse.from(commits.map(this::mapToCommitResponse));
    }

    public PageResponse<PullRequestResponse> listPullRequests(
            Long projectId, Long repositoryId, String state, String issueKey, Pageable pageable) {
        validateProjectAndRepository(projectId, repositoryId);
        GitHubPullRequestState parsedState = (state != null && !state.isBlank())
                ? GitHubPullRequestState.valueOf(state.trim().toUpperCase())
                : null;
        Page<GitHubPullRequest> prs = pullRequestRepository.findByRepositoryIdAndFilter(repositoryId, parsedState, issueKey, pageable);
        return PageResponse.from(prs.map(this::mapToPullRequestResponse));
    }

    public PageResponse<GitHubActivityResponse> listActivities(
            Long projectId, Long actorUserId, String type, String issueKey, Instant from, Instant to, Pageable pageable) {
        validateProject(projectId);

        List<GitHubActivityResponse> activities = new ArrayList<>();
        boolean includeCommits = type == null || "COMMIT".equalsIgnoreCase(type);
        boolean includePrs = type == null || "PULL_REQUEST".equalsIgnoreCase(type);

        if (includeCommits) {
            Page<GitHubCommit> commits = commitRepository.findUnifiedActivity(projectId, actorUserId, issueKey, from, to, Pageable.unpaged());
            commits.forEach(c -> activities.add(mapCommitToActivity(c)));
        }

        if (includePrs) {
            Page<GitHubPullRequest> prs = pullRequestRepository.findUnifiedActivity(projectId, actorUserId, null, issueKey, from, to, Pageable.unpaged());
            prs.forEach(pr -> activities.add(mapPrToActivity(pr)));
        }

        activities.sort(Comparator.comparing(GitHubActivityResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), activities.size());
        List<GitHubActivityResponse> pagedList = (start > activities.size()) ? Collections.emptyList() : activities.subList(start, end);

        Page<GitHubActivityResponse> resultPage = new PageImpl<>(pagedList, pageable, activities.size());
        return PageResponse.from(resultPage);
    }

    public PageResponse<GitHubActivityResponse> listTaskActivities(Long projectId, Long taskId, Pageable pageable) {
        validateProject(projectId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task không tồn tại: " + taskId));
        if (!projectId.equals(task.getProjectId())) {
            throw new IllegalArgumentException("Task không thuộc project: " + projectId);
        }

        List<GitHubActivityResponse> activities = new ArrayList<>();
        List<GitHubCommit> commits = commitRepository.findByTaskId(taskId);
        commits.forEach(c -> activities.add(mapCommitToActivity(c)));

        List<GitHubPullRequest> prs = pullRequestRepository.findByTaskId(taskId);
        prs.forEach(pr -> activities.add(mapPrToActivity(pr)));

        activities.sort(Comparator.comparing(GitHubActivityResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), activities.size());
        List<GitHubActivityResponse> pagedList = (start > activities.size()) ? Collections.emptyList() : activities.subList(start, end);

        return PageResponse.from(new PageImpl<>(pagedList, pageable, activities.size()));
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
        List<String> keys = new ArrayList<>(GitHubTaskLinkService.extractIssueKeys(pr.getHeadRef() + " " + pr.getTitle() + " " + pr.getBody()));
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
                commit.getAuthorExternalAccountId(),
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
        List<String> keys = new ArrayList<>(GitHubTaskLinkService.extractIssueKeys(pr.getHeadRef() + " " + pr.getTitle() + " " + pr.getBody()));
        return new GitHubActivityResponse(
                "PULL_REQUEST",
                String.valueOf(pr.getNumber()),
                pr.getTitle(),
                pr.getAuthorExternalAccountId(),
                pr.getAuthorLogin(),
                pr.getCreatedAt(),
                pr.getHtmlUrl(),
                keys,
                linkedTaskIds
        );
    }
}
