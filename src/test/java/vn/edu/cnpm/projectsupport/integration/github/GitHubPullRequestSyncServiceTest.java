package vn.edu.cnpm.projectsupport.integration.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubPullRequestSyncServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private GitHubPullRequestRepository pullRequestRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    @InjectMocks
    private GitHubPullRequestSyncService syncService;

    private GitHubPullRequestDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = new GitHubPullRequestDto();
        sampleDto.setId(100L);
        sampleDto.setNumber(1);
        sampleDto.setTitle("Test PR");
        sampleDto.setState("open");
        sampleDto.setHtmlUrl("https://github.com/test/repo/pull/1");
    }

    @Test
    void syncPullRequests_OpenState() throws Exception {
        sampleDto.setState("open");
        when(gitHubClient.getPullRequests(anyString(), anyString(), anyString(), eq(1), eq(100)))
                .thenReturn(List.of(sampleDto))
                .thenReturn(Collections.emptyList());

        when(pullRequestRepository.findByRepositoryIdAndNumber(1L, 1))
                .thenReturn(Optional.empty());

        syncService.syncPullRequests(1L, 1L, "owner", "repo", "token", "corr-id");

        verify(pullRequestRepository, times(1)).save(any(GitHubPullRequest.class));
    }

    @Test
    void syncPullRequests_ClosedAndMergedState() throws Exception {
        sampleDto.setState("closed");
        sampleDto.setMergedAt(Instant.now());
        
        when(gitHubClient.getPullRequests(anyString(), anyString(), anyString(), eq(1), eq(100)))
                .thenReturn(List.of(sampleDto))
                .thenReturn(Collections.emptyList());

        when(pullRequestRepository.findByRepositoryIdAndNumber(1L, 1))
                .thenReturn(Optional.empty());

        syncService.syncPullRequests(1L, 1L, "owner", "repo", "token", "corr-id");

        verify(pullRequestRepository, times(1)).save(any(GitHubPullRequest.class));
    }

    @Test
    void syncPullRequests_DuplicateUpsert() throws Exception {
        GitHubPullRequest existingPr = new GitHubPullRequest(1L, 1, "Old Title", "main", "feature", "OPEN", "url");
        
        when(gitHubClient.getPullRequests(anyString(), anyString(), anyString(), eq(1), eq(100)))
                .thenReturn(List.of(sampleDto))
                .thenReturn(Collections.emptyList());

        when(pullRequestRepository.findByRepositoryIdAndNumber(1L, 1))
                .thenReturn(Optional.of(existingPr));

        syncService.syncPullRequests(1L, 1L, "owner", "repo", "token", "corr-id");

        assertEquals("Test PR", existingPr.getTitle());
        verify(pullRequestRepository, times(1)).save(existingPr);
    }

    @Test
    void syncPullRequests_Pagination() throws Exception {
        GitHubPullRequestDto dtoPage2 = new GitHubPullRequestDto();
        dtoPage2.setId(101L);
        dtoPage2.setNumber(2);
        dtoPage2.setTitle("PR Page 2");
        dtoPage2.setState("open");
        dtoPage2.setHtmlUrl("https://github.com/test/repo/pull/2");

        when(gitHubClient.getPullRequests(anyString(), anyString(), anyString(), eq(1), eq(100)))
                .thenReturn(List.of(sampleDto));
        when(gitHubClient.getPullRequests(anyString(), anyString(), anyString(), eq(2), eq(100)))
                .thenReturn(List.of(dtoPage2))
                .thenReturn(Collections.emptyList());

        when(pullRequestRepository.findByRepositoryIdAndNumber(anyLong(), anyInt()))
                .thenReturn(Optional.empty());

        syncService.syncPullRequests(1L, 1L, "owner", "repo", "token", "corr-id");

        verify(gitHubClient, times(3)).getPullRequests(anyString(), anyString(), anyString(), anyInt(), eq(100));
        verify(pullRequestRepository, times(2)).save(any(GitHubPullRequest.class));
    }
}