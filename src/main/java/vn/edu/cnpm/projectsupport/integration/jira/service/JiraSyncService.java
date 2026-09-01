package vn.edu.cnpm.projectsupport.integration.jira.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraProject;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraBacklogSnapshot;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssueSnapshot;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.pagination.JiraPaginationReader;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraBacklogSnapshotRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueSnapshotRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.sprint.domain.Sprint;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;

@Service
public class JiraSyncService {

    private static final int PAGE_SIZE = 50;

    private final ProjectRepository projectRepository;
    private final JiraClient jiraClient;
    private final JiraIssueSnapshotRepository issueSnapshotRepository;
    private final JiraBacklogSnapshotRepository backlogSnapshotRepository;
    private final SprintRepository sprintRepository;
    private final SyncLogRepository syncLogRepository;
    private final JiraPaginationReader paginationReader;

    public JiraSyncService(
            ProjectRepository projectRepository,
            JiraClient jiraClient,
            JiraIssueSnapshotRepository issueSnapshotRepository,
            JiraBacklogSnapshotRepository backlogSnapshotRepository,
            SprintRepository sprintRepository,
            SyncLogRepository syncLogRepository) {

        this.projectRepository = projectRepository;
        this.jiraClient = jiraClient;
        this.issueSnapshotRepository = issueSnapshotRepository;
        this.backlogSnapshotRepository = backlogSnapshotRepository;
        this.sprintRepository = sprintRepository;
        this.syncLogRepository = syncLogRepository;
        this.paginationReader = new JiraPaginationReader(PAGE_SIZE);
    }

    public JiraSyncResult syncProject(Long projectId) {
        String correlationId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        Project project = projectRepository.findById(projectId).orElse(null);
        String projectKey = project == null ? null : project.getJiraProjectKey();

        SyncLog log = new SyncLog(
                project == null ? null : projectId,
                IntegrationProvider.JIRA,
                "PROJECT_SYNC",
                projectKey,
                SyncDirection.IMPORT,
                correlationId,
                startedAt);
        syncLogRepository.save(log);

        if (project == null) {
            log.setStatus(SyncLogStatus.FAILED);
            log.setErrorCode("PROJECT_NOT_FOUND");
            log.setErrorMessage("Project không tồn tại");
            log.setCompletedAt(Instant.now());
            syncLogRepository.save(log);
            throw new IllegalArgumentException("Project không tồn tại");
        }

        if (projectKey == null || projectKey.isBlank()) {
            log.setStatus(SyncLogStatus.FAILED);
            log.setErrorCode("JIRA_PROJECT_KEY_MISSING");
            log.setErrorMessage("Project chưa được cấu hình Jira Project Key");
            log.setCompletedAt(Instant.now());
            syncLogRepository.save(log);
            throw new IllegalArgumentException("Project chưa được cấu hình Jira Project Key");
        }

        int[] counters = new int[3];
        int[] errors = new int[1];

        Instant syncedAt = Instant.now();

        try {
            JiraProject jiraProject = jiraClient.getProject(projectId, projectKey);

            updateProjectSnapshot(project, jiraProject, syncedAt);
            projectRepository.save(project);

            try {
                syncIssues(projectId, projectKey, syncedAt, correlationId, counters, errors);
            } catch (RuntimeException e) {
                errors[0]++;
                saveError(projectId, "ISSUE", projectKey, correlationId, e);
            }

            try {
                syncBacklog(projectId, projectKey, syncedAt, counters, errors);
            } catch (RuntimeException e) {
                errors[0]++;
                saveError(projectId, "BACKLOG", projectKey, correlationId, e);
            }

            try {
                syncSprints(projectId, projectKey, syncedAt, correlationId, counters, errors);
            } catch (RuntimeException e) {
                errors[0]++;
                saveError(projectId, "SPRINT", projectKey, correlationId, e);
            }

            project.setJiraLastSyncedAt(syncedAt);
            projectRepository.save(project);

            log.setStatus(errors[0] == 0 ? SyncLogStatus.SUCCESS : SyncLogStatus.FAILED);

            if (errors[0] > 0) {
                log.setErrorCode("PARTIAL_SYNC");
                log.setErrorMessage("Một phần dữ liệu Jira đồng bộ thất bại");
            }

            log.setCompletedAt(Instant.now());
            syncLogRepository.save(log);

            return new JiraSyncResult(projectId, jiraProject.key(), counters[0], counters[1], counters[2], errors[0], syncedAt, correlationId);

        } catch (RuntimeException e) {
            log.setStatus(SyncLogStatus.FAILED);
            log.setErrorCode("SYNC_FAILED");
            log.setErrorMessage(safeMessage(e));
            log.setCompletedAt(Instant.now());

            syncLogRepository.save(log);
            throw e;
        }
    }

    private void updateProjectSnapshot(Project project, JiraProject jiraProject, Instant syncedAt) {

        project.setJiraProjectId(jiraProject.id());
        project.setJiraProjectKey(jiraProject.key());
        project.setJiraSiteUrl(extractOrigin(jiraProject.self()));
        project.setJiraLastSyncedAt(syncedAt);
    }

    private void syncIssues(Long projectId, String projectKey, Instant syncedAt, String correlationId, int[] counters, int[] errors) {
        paginationReader.readPages((start, size) ->
                        jiraClient.getIssues(projectId, projectKey, start, size),
                page -> {
                    for (JiraIssueDto dto : page) {
                        try {
                            upsertIssue(projectId, dto, syncedAt);
                            counters[0]++;
                        } catch (RuntimeException e) {
                            errors[0]++;
                            saveError(projectId, "ISSUE", dto.key(), correlationId, e);
                        }
                    }
                });
    }

    private void upsertIssue(Long projectId, JiraIssueDto dto, Instant syncedAt) {
        if (dto.id() == null || dto.key() == null) {
            throw new IllegalArgumentException("Jira issue thiếu id hoặc key");
        }

        JiraIssueSnapshot issue = issueSnapshotRepository.findByProjectIdAndJiraIssueId(projectId, dto.id()).orElseGet(() ->
            issueSnapshotRepository.findByProjectIdAndJiraIssueKey(projectId,dto.key()).orElseGet(() ->
                new JiraIssueSnapshot(projectId, dto.id(), dto.key())));

        issue.setSummary(dto.summary());
        issue.setIssueType(dto.fields() == null || dto.fields().issuetype() == null ? null : dto.fields().issuetype().name());
        issue.setStatus(dto.status() == null ? null : dto.status().name());

        issue.setUrl("/browse/" + dto.key());
        issue.setRemoteUpdatedAt(parseInstant(dto.updated()));
        issue.setLastSyncedAt(syncedAt);

        Map<String, Object> raw = snapshotMap(dto);

        issue.setSnapshotHash(hash(raw));
        issue.setRawSnapshot(raw);

        issueSnapshotRepository.save(issue);
    }

    private void syncBacklog(Long projectId, String projectKey, Instant syncedAt, int[] counters, int[] errors) {
        List<Map<String, Object>> all = new ArrayList<>();

        paginationReader.readPages((start, size) -> jiraClient.getBacklog(projectId, projectKey, start, size),
                page -> {
                    for (JiraIssueDto dto : page) {
                        all.add(snapshotMap(dto));
                        counters[1]++;
                    }
                });

        JiraBacklogSnapshot snapshot = backlogSnapshotRepository.findByProjectId(projectId).orElseGet(() -> new JiraBacklogSnapshot(projectId, projectKey));

        snapshot.setJiraProjectKey(projectKey);
        Map<String, Object> raw = new LinkedHashMap<>();

        raw.put("projectKey", projectKey);
        raw.put("items", all);
        raw.put("count", all.size());

        snapshot.setLastSyncedAt(syncedAt);
        snapshot.setSnapshotHash(hash(raw));
        snapshot.setRawSnapshot(raw);

        backlogSnapshotRepository.save(snapshot);
    }

    private void syncSprints(Long projectId, String projectKey, Instant syncedAt, String correlationId, int[] counters, int[] errors) {
        paginationReader.readPages((start, size) -> getSprintPage(projectId, projectKey, start, size),
                page -> {
                    for (JiraSprintDto dto : page) {
                        try {
                            upsertSprint(projectId, dto, syncedAt);
                            counters[2]++;
                        } catch (RuntimeException e) {
                            errors[0]++;
                            saveError(projectId, "SPRINT", dto.id(), correlationId, e);
                        }
                    }
                });
    }

    private JiraPageDto<JiraSprintDto> getSprintPage(Long projectId, String projectKey, int start, int size) {
        JiraSprintPageDto page = jiraClient.getSprints(projectId, projectKey, start, size);
        return new JiraPageDto<>(page.startAt(), page.maxResults(), page.total(), page.isLast(), page.values());
    }

    private void upsertSprint(Long projectId, JiraSprintDto dto,Instant syncedAt) {
        Long jiraId = parseLong(dto.id());

        if (jiraId == null || dto.name() == null) {
            throw new IllegalArgumentException("Jira sprint thiếu id hoặc name");
        }

        Sprint sprint = sprintRepository.findByProjectIdAndJiraSprintId( projectId, jiraId).orElseGet(() ->
        new Sprint(projectId,dto.name(),dto.state() == null ? "UNKNOWN": dto.state()));

        sprint.setJiraSprintId(jiraId);
        sprint.setName(dto.name());
        sprint.setState(dto.state() == null ? "UNKNOWN" : dto.state());
        sprint.setGoal(dto.goal());
        sprint.setStartDate(parseInstant(dto.startDate()));
        sprint.setEndDate(parseInstant(dto.endDate()));
        sprint.setLastSyncedAt(syncedAt);

        sprintRepository.save(sprint);
    }

    private void saveError(Long projectId, String entityType, String entityId, String correlationId, RuntimeException e) {

        SyncLog item = new SyncLog(projectId,IntegrationProvider.JIRA, entityType, entityId, SyncDirection.IMPORT, correlationId,Instant.now());

        item.setStatus(SyncLogStatus.FAILED);
        item.setErrorCode("ITEM_SYNC_FAILED");
        item.setErrorMessage(safeMessage(e));
        item.setCompletedAt(Instant.now());

        syncLogRepository.save(item);
    }

    private Map<String, Object> snapshotMap(JiraIssueDto dto) {

        Map<String, Object> raw = new LinkedHashMap<>();

        raw.put("id", dto.id());
        raw.put("key", dto.key());
        raw.put("summary", dto.summary());

        raw.put(
                "status",
                dto.status() == null
                        ? null
                        : dto.status().name());

        raw.put(
                "priority",
                dto.priority() == null
                        ? null
                        : dto.priority().name());

        raw.put(
                "issueType",
                dto.fields() == null
                        || dto.fields().issuetype() == null
                        ? null
                        : dto.fields().issuetype().name());

        raw.put(
                "assignee",
                dto.assignee() == null
                        ? null
                        : dto.assignee().displayName());

        raw.put("updated", dto.updated());

        return raw;
    }

    private String extractOrigin(String self) {

        if (self == null || self.isBlank()) {
            return null;
        }

        try {
            java.net.URI uri =
                    java.net.URI.create(self);

            return uri.getScheme()
                    + "://"
                    + uri.getAuthority();

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Instant parseInstant(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).toInstant();

        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value, DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSZ")).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private Long parseLong(String value) {

        try {
            return value == null ? null : Long.valueOf(value);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String hash(Object value) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8)));

        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo snapshot hash", e);
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();

        return message == null ? e.getClass().getSimpleName(): message.substring(0, Math.min(message.length(), 1000));
    }
}