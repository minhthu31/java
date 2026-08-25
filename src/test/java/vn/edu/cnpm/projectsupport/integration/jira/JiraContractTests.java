package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionRequest;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;
import vn.edu.cnpm.projectsupport.task.TaskController;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.service.TaskService;

class JiraContractTests {

    private static final Path OPENAPI = Path.of(
            "docs", "api", "jira-integration-v1.openapi.yaml");
    private static final Path MAPPING = Path.of(
            "docs", "integrations", "CNPM-74-jira-field-mapping-and-api-contract.md");

    @Test
    void syncStatusUsesTheSingleSprint3StateMachine() {
        assertThat(Arrays.stream(SyncStatus.values()).map(Enum::name))
                .containsExactly("NOT_SYNCED", "SYNCING", "SYNCED", "SYNC_FAILED");
    }

    @Test
    void openApiContainsEveryAcceptedJiraIntegrationOperation() throws Exception {
        String contract = Files.readString(OPENAPI);

        assertThat(contract)
                .contains("openapi: 3.0.3")
                .contains("version: 1.0.0")
                .contains("/projects/{projectId}/integrations/jira/config:")
                .contains("/projects/{projectId}/integrations/jira/test-connection:")
                .contains("/projects/{projectId}/integrations/jira/tasks/{taskId}/sync:")
                .contains("/projects/{projectId}/integrations/jira/tasks/{taskId}/retry:")
                .contains("/projects/{projectId}/integrations/jira/issues/{jiraIssueKey}:")
                .contains("enum: [NOT_SYNCED, SYNCING, SYNCED, SYNC_FAILED]")
                .contains("name: Idempotency-Key")
                .contains("required: true")
                .contains("writeOnly: true");
    }

    @Test
    void mappingCoversAllFieldsRequiredByCnpm74() throws Exception {
        String mapping = Files.readString(MAPPING);

        assertThat(mapping)
                .contains("`title`")
                .contains("`description`")
                .contains("`issueType`")
                .contains("`priority`")
                .contains("`assigneeUserId`")
                .contains("`deadline`")
                .contains("`sprintId`")
                .contains("`featureId`")
                .contains("Atlassian Document Format")
                .contains("accountId")
                .contains("fields.parent.key")
                .contains("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    void connectionResponseCannotExposeCredentialFields() {
        assertThat(Arrays.stream(JiraConnectionRequest.class.getRecordComponents())
                .map(RecordComponent::getName))
                .contains("email", "apiToken");

        assertThat(Arrays.stream(JiraConnectionResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("email", "apiToken", "encryptedSecret");
    }

    @Test
    void integrationServiceMatchesTheOpenApiOperationBoundary() throws Exception {
        assertThat(JiraIntegrationService.class.getMethods())
                .extracting(method -> method.getName())
                .containsExactlyInAnyOrder(
                        "getConnection",
                        "configureConnection",
                        "testConnection",
                        "syncTask",
                        "retryTaskSync",
                        "getIssue");
    }

    @Test
    void cnpm60AndCnpm61StillShareTheIdempotentCreateTaskSignature() throws Exception {
        assertThat(TaskService.class.getMethod(
                "createTask", Long.class, CreateTaskRequest.class, String.class))
                .isNotNull();

        var controllerMethod = TaskController.class.getDeclaredMethod(
                "createTask", Long.class, String.class, CreateTaskRequest.class);
        assertThat(controllerMethod.getAnnotation(PostMapping.class)).isNotNull();
    }
}
