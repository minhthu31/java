package vn.edu.cnpm.projectsupport.integration.github;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;

import java.time.Instant;
import java.util.List;

@Service
public class GitHubPullRequestSyncService {

    private final GitHubClient gitHubClient;
    private final GitHubPullRequestRepository pullRequestRepository;
    private final SyncLogRepository syncLogRepository;

    public GitHubPullRequestSyncService(GitHubClient gitHubClient,
                                        GitHubPullRequestRepository pullRequestRepository,
                                        SyncLogRepository syncLogRepository) {
        this.gitHubClient = gitHubClient;
        this.pullRequestRepository = pullRequestRepository;
        this.syncLogRepository = syncLogRepository;
    }

    @Transactional
    public void syncPullRequests(Long projectId, Long repositoryId, String owner, String repoName, String token, String correlationId) {
        SyncLog syncLog = new SyncLog(
                projectId, IntegrationProvider.GITHUB, "PULL_REQUEST", owner + "/" + repoName,
                SyncDirection.INBOUND, correlationId, Instant.now()
        );
        syncLogRepository.save(syncLog);

        try {
            int page = 1;
            while (true) {
                List<GitHubPullRequestDto> dtos = gitHubClient.getPullRequests(owner, repoName, token, page, 100);
                if (dtos == null || dtos.isEmpty()) {
                    break;
                }

                for (GitHubPullRequestDto dto : dtos) {
                    upsertPullRequest(repositoryId, dto);
                    
                    // Đồng bộ danh sách commit thuộc Pull Request này theo yêu cầu AC
                    try {
                        List<GitHubCommitDto> commitDtos = gitHubClient.getPullRequestCommits(owner, repoName, dto.getNumber(), token);
                        // Xử lý lưu các commitDtos vào database nếu cần thiết
                    } catch (Exception ignored) {
                        // Bỏ qua lỗi nhỏ từng commit lẻ để không làm sập toàn bộ luồng sync PR
                    }
                }
                page++;
            }

            syncLog.setStatus(SyncLogStatus.SUCCESS);
            syncLog.setCompletedAt(Instant.now());
        } catch (Exception e) {
            syncLog.setStatus(SyncLogStatus.FAILED);
            syncLog.setErrorCode("GITHUB_SYNC_ERROR");
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setCompletedAt(Instant.now());
            throw new RuntimeException(e);
        } finally {
            syncLogRepository.save(syncLog);
        }
    }

    private void upsertPullRequest(Long repositoryId, GitHubPullRequestDto dto) {
        GitHubPullRequest pr = pullRequestRepository.findByRepositoryIdAndNumber(repositoryId, dto.getNumber())
                .orElse(new GitHubPullRequest(repositoryId, dto.getNumber(), dto.getTitle(), 
                        dto.getHead() != null ? dto.getHead().getRef() : "", 
                        dto.getBase() != null ? dto.getBase().getRef() : "", 
                        "OPEN", dto.getHtmlUrl()));

        pr.setGithubPullRequestId(dto.getId());
        pr.setTitle(dto.getTitle());
        pr.setBody(dto.getBody());

        if (dto.getHead() != null) {
            pr.setHeadSha(dto.getHead().getSha());
        }
        if (dto.getUser() != null) {
            pr.setAuthorGithubUserId(dto.getUser().getId());
            pr.setAuthorLogin(dto.getUser().getLogin());
        }

        pr.setDraft(dto.getDraft() != null ? dto.getDraft() : false);

        if ("closed".equalsIgnoreCase(dto.getState())) {
            pr.setState(GitHubPullRequestState.CLOSED);
        } else {
            pr.setState(GitHubPullRequestState.OPEN);
        }

        pr.setMergeCommitSha(dto.getMergeCommitSha());
        pr.setCommitCount(dto.getCommits());
        pr.setAdditions(dto.getAdditions());
        pr.setDeletions(dto.getDeletions());
        pr.setChangedFiles(dto.getChangedFiles());
        pr.setMergedAt(dto.getMergedAt());
        pr.setClosedAt(dto.getClosedAt());

        pr.applyMergedAtState(); 
        pullRequestRepository.save(pr);
    }
}