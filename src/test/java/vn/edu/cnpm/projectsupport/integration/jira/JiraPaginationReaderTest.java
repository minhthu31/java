package vn.edu.cnpm.projectsupport.integration.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.pagination.JiraPaginationReader;
import vn.edu.cnpm.projectsupport.integration.jira.pagination.JiraPaginationReader.JiraPaginationLimitException;

class JiraPaginationReaderTest {

    @Test
    void shouldReadAllPagesUsingJiraMetadata() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        List<String> result = reader.readAll((startAt, maxResults) -> {
            if (startAt == 0) {
                return new JiraPageDto<>(0, 3, 7, false, List.of("A", "B", "C"));
            }
            if (startAt == 3) {
                return new JiraPageDto<>(3, 3, 7, false, List.of("D", "E", "F"));
            }
            return new JiraPageDto<>(6, 1, 7, true, List.of("G"));
        });

        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G"), result);
    }

    @Test
    void shouldReadMoreThanConfiguredLegacyLimitWithoutLimitWhenUsingDefaultConstructor() {
        JiraPaginationReader reader = new JiraPaginationReader(100);
        AtomicInteger calls = new AtomicInteger();

        List<Integer> result = reader.readAll((startAt, maxResults) -> {
            calls.incrementAndGet();
            int end = Math.min(startAt + maxResults, 1100);
            List<Integer> items = new ArrayList<>();
            for (int i = startAt; i < end; i++) {
                items.add(i);
            }
            return new JiraPageDto<>(startAt, maxResults, 1100, end == 1100, items);
        });

        assertEquals(1100, result.size());
        assertEquals(11, calls.get());
        assertEquals(1099, result.get(1099));
    }

    @Test
    void shouldReportLegacyLimitInsteadOfSilentlyTruncating() {
        JiraPaginationReader reader = new JiraPaginationReader(3, 5);

        assertThrows(JiraPaginationLimitException.class, () ->
                reader.readAll((startAt, maxResults) -> {
                    if (startAt == 0) {
                        return new JiraPageDto<>(0, 3, 100, false, List.of("A", "B", "C"));
                    }
                    return new JiraPageDto<>(3, 3, 100, false, List.of("D", "E", "F"));
                }));
    }

    @Test
    void shouldStopWhenJiraMarksLastPage() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        List<String> result = reader.readAll((startAt, maxResults) ->
                new JiraPageDto<>(0, 3, 100, true, List.of("A", "B")));
        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void shouldStopWhenTotalHasBeenRead() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        List<String> result = reader.readAll((startAt, maxResults) ->
                new JiraPageDto<>(0, 3, 2, false, List.of("A", "B")));
        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void shouldRejectUnexpectedStartAt() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        assertThrows(IllegalStateException.class, () ->
                reader.readAll((startAt, maxResults) ->
                        new JiraPageDto<>(10, 3, 20, false, List.of("A"))));
    }

    @Test
    void shouldRejectPageLargerThanMetadata() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        assertThrows(IllegalStateException.class, () ->
                reader.readAll((startAt, maxResults) ->
                        new JiraPageDto<>(0, 2, 20, false, List.of("A", "B", "C"))));
    }

    @Test
    void shouldRejectEmptyPageBeforeTotalIsReached() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        assertThrows(IllegalStateException.class, () ->
                reader.readAll((startAt, maxResults) ->
                        new JiraPageDto<>(0, 3, 20, false, List.of())));
    }
}
