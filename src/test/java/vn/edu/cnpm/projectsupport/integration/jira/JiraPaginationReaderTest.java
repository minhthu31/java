package vn.edu.cnpm.projectsupport.integration.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.pagination.JiraPaginationReader;

class JiraPaginationReaderTest {

    @Test
    void shouldReadAllPagesUsingJiraMetadata() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        List<String> result = reader.readAll((startAt, maxResults) -> {
                    if (startAt == 0) {
                        return new JiraPageDto<>(0, 3, 7, false,List.of("A", "B", "C"));
                    }

                    if (startAt == 3) {
                        return new JiraPageDto<>(3,3,7,false,List.of("D", "E", "F"));
                    }
                    return new JiraPageDto<>(6,1,7,true,List.of("G"));
                });

        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G"), result);
    }

    @Test
    void shouldStopWhenJiraMarksLastPage() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        List<String> result = reader.readAll((startAt, maxResults) -> new JiraPageDto<>(0,3,100,true,List.of("A", "B")));
        assertEquals(List.of("A", "B"),result);
    }

    @Test
    void shouldStopWhenTotalHasBeenRead() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        List<String> result = reader.readAll((startAt, maxResults) -> new JiraPageDto<>(0, 3, 2, false, List.of("A", "B")));

        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void shouldStopWhenPageIsEmpty() {
        JiraPaginationReader reader = new JiraPaginationReader(50);
        List<String> result = reader.readAll((startAt, maxResults) -> new JiraPageDto<>(0, 50, 0,false,List.of()));
        assertEquals(List.of(), result);
    }

    @Test
    void shouldApplySafeMaximum() {
        JiraPaginationReader reader = new JiraPaginationReader(3, 5);
        List<String> result = reader.readAll((startAt, maxResults) -> {
                    if (startAt == 0) {
                        return new JiraPageDto<>(0, 3, 100,false,List.of("A", "B", "C"));
                    }

                    return new JiraPageDto<>(3,3, 100, false, List.of("D", "E", "F"));
                });

        assertEquals(List.of("A", "B", "C", "D", "E"), result);
    }

    @Test
    void shouldRejectUnexpectedStartAt() {

        JiraPaginationReader reader = new JiraPaginationReader(3);
        assertThrows(IllegalStateException.class,() -> 
        reader.readAll((startAt, maxResults) -> 
        new JiraPageDto<>(10,3,20,false,List.of("A"))));
    }

    @Test
    void shouldRejectPageLargerThanMetadata() {

        JiraPaginationReader reader = new JiraPaginationReader(3);
        assertThrows(IllegalStateException.class,() -> 
        reader.readAll((startAt, maxResults) -> 
        new JiraPageDto<>(0,2,20,false,List.of("A", "B", "C"))));
    }
}