package vn.edu.cnpm.projectsupport.integration.github;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLinkId;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskLinkMatchedFrom;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskLinkSource;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLinkId;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueRepository;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

/** Detects Jira issue keys in GitHub activity and creates idempotent Task links. */
@Service
public class GitHubTaskLinkService {

    static final Pattern ISSUE_KEY_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_])([A-Z][A-Z0-9_]{1,29}-[1-9][0-9]*)(?![A-Za-z0-9_])");

    private final JiraIssueRepository jiraIssueRepository;
    private final TaskRepository taskRepository;
    private final TaskCommitLinkRepository commitLinkRepository;
    private final TaskPullRequestLinkRepository pullRequestLinkRepository;

    public GitHubTaskLinkService(
            JiraIssueRepository jiraIssueRepository,
            TaskRepository taskRepository,
            TaskCommitLinkRepository commitLinkRepository,
            TaskPullRequestLinkRepository pullRequestLinkRepository) {
        this.jiraIssueRepository = jiraIssueRepository;
        this.taskRepository = taskRepository;
        this.commitLinkRepository = commitLinkRepository;
        this.pullRequestLinkRepository = pullRequestLinkRepository;
    }

    public GitHubTaskLinkResult linkCommit(Long projectId, GitHubCommit commit) {
        return linkCommit(projectId, commit, null);
    }

    /**
     * Links a commit using branch first when the caller knows it, then commit message.
     * GitHub's commit list does not expose a unique branch, so branchName may be null.
     */
    public GitHubTaskLinkResult linkCommit(Long projectId, GitHubCommit commit, String branchName) {
        requireProjectId(projectId);
        if (commit == null || commit.getId() == null) {
            throw new IllegalArgumentException("A persisted GitHub commit is required");
        }

        Map<String, TaskLinkMatchedFrom> matches = new LinkedHashMap<>();
        collect(matches, branchName, TaskLinkMatchedFrom.BRANCH);
        collect(matches, commit.getMessage(), TaskLinkMatchedFrom.COMMIT_MESSAGE);

        return createLinks(projectId, matches, (taskId, matchedFrom) -> {
            TaskCommitLinkId id = new TaskCommitLinkId(taskId, commit.getId());
            if (commitLinkRepository.existsById(id)) {
                return false;
            }
            TaskCommitLink link = new TaskCommitLink(id, TaskLinkSource.AUTO);
            link.setMatchedFrom(matchedFrom);
            try {
                commitLinkRepository.saveAndFlush(link);
                return true;
            } catch (DataIntegrityViolationException duplicateRace) {
                if (commitLinkRepository.existsById(id)) {
                    return false;
                }
                throw duplicateRace;
            }
        });
    }

    /** Links a PR using the contract order: head branch, title, then body. */
    public GitHubTaskLinkResult linkPullRequest(Long projectId, GitHubPullRequest pullRequest) {
        requireProjectId(projectId);
        if (pullRequest == null || pullRequest.getId() == null) {
            throw new IllegalArgumentException("A persisted GitHub pull request is required");
        }

        Map<String, TaskLinkMatchedFrom> matches = new LinkedHashMap<>();
        collect(matches, pullRequest.getHeadRef(), TaskLinkMatchedFrom.BRANCH);
        collect(matches, pullRequest.getTitle(), TaskLinkMatchedFrom.PR_TITLE);
        collect(matches, pullRequest.getBody(), TaskLinkMatchedFrom.PR_BODY);

        return createLinks(projectId, matches, (taskId, matchedFrom) -> {
            TaskPullRequestLinkId id = new TaskPullRequestLinkId(taskId, pullRequest.getId());
            if (pullRequestLinkRepository.existsById(id)) {
                return false;
            }
            TaskPullRequestLink link = new TaskPullRequestLink(id, TaskLinkSource.AUTO);
            link.setMatchedFrom(matchedFrom);
            try {
                pullRequestLinkRepository.saveAndFlush(link);
                return true;
            } catch (DataIntegrityViolationException duplicateRace) {
                if (pullRequestLinkRepository.existsById(id)) {
                    return false;
                }
                throw duplicateRace;
            }
        });
    }

    static Set<String> extractIssueKeys(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = ISSUE_KEY_PATTERN.matcher(text);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    private void collect(
            Map<String, TaskLinkMatchedFrom> matches,
            String text,
            TaskLinkMatchedFrom matchedFrom) {
        for (String key : extractIssueKeys(text)) {
            matches.putIfAbsent(key, matchedFrom);
        }
    }

    private GitHubTaskLinkResult createLinks(
            Long projectId,
            Map<String, TaskLinkMatchedFrom> matches,
            LinkCreator linkCreator) {
        int created = 0;
        int duplicates = 0;
        List<String> warnings = new ArrayList<>();

        for (Map.Entry<String, TaskLinkMatchedFrom> match : matches.entrySet()) {
            String issueKey = match.getKey();
            JiraIssue jiraIssue = jiraIssueRepository.findByJiraIssueKey(issueKey).orElse(null);
            if (jiraIssue == null) {
                warnings.add("Jira Issue Key không tồn tại: " + issueKey);
                continue;
            }

            Task task = taskRepository.findById(jiraIssue.getTaskId()).orElse(null);
            if (task == null) {
                warnings.add("Task của Jira Issue Key không tồn tại: " + issueKey);
                continue;
            }
            if (!projectId.equals(task.getProjectId())) {
                warnings.add("Jira Issue Key không thuộc project hiện tại: " + issueKey);
                continue;
            }

            if (linkCreator.create(task.getId(), match.getValue())) {
                created++;
            } else {
                duplicates++;
            }
        }

        return new GitHubTaskLinkResult(matches.size(), created, duplicates, warnings);
    }

    private void requireProjectId(Long projectId) {
        if (projectId == null || projectId < 1) {
            throw new IllegalArgumentException("projectId must be positive");
        }
    }

    @FunctionalInterface
    private interface LinkCreator {
        boolean create(Long taskId, TaskLinkMatchedFrom matchedFrom);
    }
}
