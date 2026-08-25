package vn.edu.cnpm.projectsupport.integration.jira;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.cnpm.projectsupport.integration.jira.pagination.JiraPaginationReader;
class JiraPaginationReaderTest {
    @Test
    void shouldReadAllPages() {
        JiraPaginationReader reader = new JiraPaginationReader(3);
        List<String> result =  reader.readAll((startAt, maxResults) -> {
                    if (startAt == 0) {
                        return List.of("A", "B", "C");
                    }
                    if (startAt == 3) {
                        return List.of("D", "E", "F");
                    }
                    if (startAt == 6) {
                        return List.of("G");
                    }
                    return List.of();
                });
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G"),result);
    }

    @Test
    void shouldStopWhenPageIsSmallerThanPageSize() {
    JiraPaginationReader reader = new JiraPaginationReader(3);
    List<String> result = reader.readAll((startAt, maxResults) -> {
                if (startAt == 0) {
                    return List.of("A", "B", "C");
                }
                if (startAt == 3) {
                    return List.of("D");
                }
                throw new AssertionError("Reader should stop after last page");
            });

    assertEquals(List.of("A", "B", "C", "D"), result);
    }
    
    @Test
    void shouldReturnEmptyListWhenJiraReturnsEmptyPage() {  
    JiraPaginationReader reader = new JiraPaginationReader(50);
    List<String> result = reader.readAll((startAt, maxResults) -> List.of());
    assertEquals(List.of(),result);
    }
}