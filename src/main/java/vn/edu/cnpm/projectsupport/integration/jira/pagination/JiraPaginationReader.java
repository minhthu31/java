package vn.edu.cnpm.projectsupport.integration.jira.pagination;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class JiraPaginationReader {
    private final int pageSize;
    public JiraPaginationReader(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        this.pageSize = pageSize;
    }

    public <T> List<T> readAll(BiFunction<Integer, Integer, List<T>> pageLoader) {
        List<T> result = new ArrayList<>();
        int startAt = 0;

        while (true) {
            List<T> page = pageLoader.apply(startAt, pageSize);
            if (page == null || page.isEmpty()) {
                break;
            }
            result.addAll(page);
            if (page.size() < pageSize) {
                break;
            }
            startAt += pageSize;
        }
        return result;
    }
}