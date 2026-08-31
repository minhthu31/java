package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraTaskSyncResponse;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfigStatus;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueRequest;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;

class JiraIntegrationServiceImplTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String PROJECT_KEY = "PROJ";
    private static final String BASE_URL = "https://example.atlassian.net";
    private static final String VALID_IDEMPOTENCY_KEY = "idemp-key-12345678";

    // =========================================================================
    // 1. KIỂM THỬ IDEMPOTENCY (CHỐNG TẠO TRÙNG & GỌI LẠI REQUEST)
    // =========================================================================
    @Nested
    @DisplayName("Tests for Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Gửi lại cùng Idempotency Key khi đã hoàn tất -> Trả về kết quả SyncLog cũ không gọi Jira")
        void syncTask_WhenSameIdempotencyKeyCompleted_ReturnsPreviousSyncLogResponse() {
            SyncLog completedLog = new SyncLog();
            completedLog.setStatus(SyncLogStatus.SUCCESS);
            completedLog.setRequestFingerprint("matching-fingerprint");

            // Giả lập fingerprint khớp với task hiện tại
            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    eq(PROJECT_ID), eq("TASK"), eq(String.valueOf(TASK_ID)), eq(VALID_IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.of(completedLog));

            JiraIssue issue = new JiraIssue(TASK_ID, "10001", "PROJ-10", BASE_URL + "/browse/PROJ-10", Instant.now());
            when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.of(issue));

            JiraTaskSyncResponse response = service.syncTask(PROJECT_ID, TASK_ID, VALID_IDEMPOTENCY_KEY);

            assertThat(response.jiraIssueKey()).isEqualTo("PROJ-10");
            assertThat(response.syncStatus()).isEqualTo(SyncStatus.SYNCED);
            verify(jiraClient, never()).createIssue(any(), any(), any());
        }

        @Test
        @DisplayName("Tái sử dụng Idempotency Key cho Task có nội dung khác -> Ném lỗi IDEMPOTENCY_KEY_REUSED (409)")
        void syncTask_WhenIdempotencyKeyReusedWithDifferentData_ThrowsConflictException() {
            SyncLog differentLog = new SyncLog();
            differentLog.setStatus(SyncLogStatus.SUCCESS);
            differentLog.setRequestFingerprint("different-payload-fingerprint");

            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    eq(PROJECT_ID), eq("TASK"), eq(String.valueOf(TASK_ID)), eq(VALID_IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.of(differentLog));

            assertThatThrownBy(() -> service.syncTask(PROJECT_ID, TASK_ID, VALID_IDEMPOTENCY_KEY))
                    .isInstanceOf(JiraApiException.class)
                    .satisfies(ex -> {
                        JiraApiException apiEx = (JiraApiException) ex;
                        assertThat(apiEx.getErrorCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }

        @Test
        @DisplayName("Task đã có Jira Issue mapping và không đổi nội dung -> Không tạo issue mới")
        void syncTask_WhenTaskAlreadyMappedAndUnchanged_ReturnsExistingMapping() {
            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    any(), any(), any(), any())).thenReturn(Optional.empty());

            JiraIssue existing = new JiraIssue(TASK_ID, "10001", "PROJ-10", BASE_URL + "/browse/PROJ-10", Instant.now());
            existing.setSnapshotHash(null); // Trường hợp snapshotHash null hoặc trùng khớp
            when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.of(existing));

            JiraTaskSyncResponse response = service.syncTask(PROJECT_ID, TASK_ID, VALID_IDEMPOTENCY_KEY);

            assertThat(response.jiraIssueKey()).isEqualTo("PROJ-10");
            verify(jiraClient, never()).createIssue(any(), any(), any());
            verify(jiraClient, never()).updateIssue(any(), any(), any(), any());
        }
    }

    // =========================================================================
    // 2. KIỂM THỬ LỖI JIRA API & HTTP ERROR CODE
    // =========================================================================
    @Nested
    @DisplayName("Tests for Jira API Errors")
    class JiraApiErrorTests {

        @Test
        @DisplayName("Jira trả về lỗi 401 Unauthorized -> Đánh dấu Task SYNC_FAILED và ghi log FAILED")
        void syncTask_WhenJiraAuthFails_MarksTaskFailedAndSyncLogFailed() {
            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    any(), any(), any(), any())).thenReturn(Optional.empty());
            when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.empty());
            when(jiraClient.findIssuesByLabel(any(), any(), any())).thenReturn(List.of());

            JiraApiException authError = new JiraApiException(
                    HttpStatus.UNAUTHORIZED, "JIRA_AUTH_FAILED", false, null, "Unauthorized token", null);
            when(jiraClient.createIssue(eq(PROJECT_ID), eq(PROJECT_KEY), any(JiraCreateIssueRequest.class)))
                    .thenThrow(authError);

            assertThatThrownBy(() -> service.syncTask(PROJECT_ID, TASK_ID, VALID_IDEMPOTENCY_KEY))
                    .isSameAs(authError);

            assertThat(task.getSyncStatus()).isEqualTo(SyncStatus.SYNC_FAILED);

            ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
            verify(syncLogRepository, times(2)).save(captor.capture());
            SyncLog failedLog = captor.getAllValues().get(1);
            assertThat(failedLog.getStatus()).isEqualTo(SyncLogStatus.FAILED);
            assertThat(failedLog.getErrorCode()).isEqualTo("JIRA_AUTH_FAILED");
        }

        @Test
        @DisplayName("Lỗi thiếu cấu hình Assignee mapping -> Ném ASSIGNEE_MAPPING_MISSING (422)")
        void syncTask_WhenAssigneeMappingMissing_ThrowsUnprocessableEntity() {
            task.setAssigneeUserId(999L);
            when(jdbcTemplate.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), eq(999L)))
                    .thenReturn(List.of()); // Không tìm thấy mapping trong DB

            assertThatThrownBy(() -> service.syncTask(PROJECT_ID, TASK_ID, VALID_IDEMPOTENCY_KEY))
                    .isInstanceOf(JiraApiException.class)
                    .satisfies(ex -> {
                        JiraApiException apiEx = (JiraApiException) ex;
                        assertThat(apiEx.getErrorCode()).isEqualTo("ASSIGNEE_MAPPING_MISSING");
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    });
        }
    }

    // =========================================================================
    // 3. KIỂM THỬ RETRY & RECONCILIATION
    // =========================================================================
    @Nested
    @DisplayName("Tests for Retry and Label Reconcile")
    class RetryAndReconcileTests {

        @Test
        @DisplayName("Retry sau khi mất kết nối: Reconcile issue bằng Label thành công -> Trả về SYNCED")
        void retryTaskSync_WhenRemoteIssueCreatedBeforeTimeout_ReconcilesSuccessfully() {
            task.setSyncStatus(SyncStatus.SYNC_FAILED);
            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    any(), any(), any(), any())).thenReturn(Optional.empty());
            when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.empty());

            // Giả lập Jira đã tạo issue nhưng local bị timeout, reconcile quét ra theo label
            JiraCreateIssueResponse discovered = new JiraCreateIssueResponse(
                    "10005", "PROJ-88", BASE_URL + "/rest/api/3/issue/10005");
            when(jiraClient.findIssuesByLabel(eq(PROJECT_ID), eq(PROJECT_KEY), eq("cnpm-local-task-" + TASK_ID)))
                    .thenReturn(List.of(discovered));

            JiraTaskSyncResponse response = service.retryTaskSync(PROJECT_ID, TASK_ID, VALID_IDEMPOTENCY_KEY);

            assertThat(response.syncStatus()).isEqualTo(SyncStatus.SYNCED);
            assertThat(response.jiraIssueKey()).isEqualTo("PROJ-88");
            assertThat(response.retryCount()).isEqualTo(2);

            verify(jiraClient, never()).createIssue(any(), any(), any()); // Không tạo mới trùng lặp
            verify(jiraIssueRepository).saveAndFlush(any(JiraIssue.class));
        }

        @Test
        @DisplayName("Không cho phép retry khi Task không ở trạng thái SYNC_FAILED hoặc NOT_SYNCED")
        void retryTaskSync_WhenTaskAlreadySynced_ThrowsClientException() {
            task.setSyncStatus(SyncStatus.SYNCED);

            assertThatThrownBy(() -> service.retryTaskSync(PROJECT_ID, TASK_ID, VALID_IDEMPOTENCY_KEY))
                    .isInstanceOf(JiraClientException.class)
                    .hasMessageContaining("Task không ở trạng thái có thể retry");
        }
    }
}
