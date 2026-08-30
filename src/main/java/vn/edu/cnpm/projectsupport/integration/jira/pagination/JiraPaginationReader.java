package vn.edu.cnpm.projectsupport.integration.jira.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;

public class JiraPaginationReader {

    private final int pageSize;
    private final Integer maxItems;

    public JiraPaginationReader(int pageSize) {
        validatePageSize(pageSize);
        this.pageSize = pageSize;
        this.maxItems = null;
    }

    /**
     * Legacy bounded reader. If the Jira result is larger than maxItems, the
     * reader fails explicitly instead of silently returning partial data.
     */
    public JiraPaginationReader(int pageSize, int maxItems) {
        validatePageSize(pageSize);
        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems must be greater than 0");
        }
        if (pageSize > maxItems) {
            throw new IllegalArgumentException("pageSize must not be greater than maxItems");
        }
        this.pageSize = pageSize;
        this.maxItems = maxItems;
    }

    public <T> void readPages(
            BiFunction<Integer, Integer, JiraPageDto<T>> pageLoader,
            Consumer<List<T>> pageConsumer) {
        int startAt = 0;
        int processed = 0;

        while (true) {
            JiraPageDto<T> page = pageLoader.apply(startAt, pageSize);
            if (page == null) {
                throw new IllegalStateException("Jira pagination returned null page");
            }

            validatePage(page, startAt);
            List<T> items = page.issues() == null ? List.of() : page.issues();

            if (maxItems != null && processed + items.size() > maxItems) {
                throw new JiraPaginationLimitException(
                        "Jira result exceeds configured maximum of " + maxItems + " items");
            }

            if (!items.isEmpty()) {
                pageConsumer.accept(items);
                processed += items.size();
            }

            if (Boolean.TRUE.equals(page.isLast()) || processed >= page.total()) {
                return;
            }

            if (items.isEmpty()) {
                throw new IllegalStateException(
                        "Jira pagination returned an empty page before reaching total");
            }

            int nextStartAt = page.startAt() + items.size();
            if (nextStartAt <= startAt) {
                throw new IllegalStateException("Jira pagination did not advance");
            }
            startAt = nextStartAt;
        }
    }

    public <T> List<T> readAll(
            BiFunction<Integer, Integer, JiraPageDto<T>> pageLoader) {
        List<T> result = new ArrayList<>();
        readPages(pageLoader, result::addAll);
        return result;
    }

    private static void validatePageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
    }

    private <T> void validatePage(JiraPageDto<T> page, int expectedStartAt) {
        if (page.startAt() != expectedStartAt) {
            throw new IllegalStateException("Jira pagination returned unexpected startAt");
        }
        if (page.maxResults() <= 0) {
            throw new IllegalStateException("Jira pagination returned invalid maxResults");
        }
        if (page.total() < 0 || page.total() < page.startAt()) {
            throw new IllegalStateException("Jira pagination returned invalid total");
        }
        List<T> items = page.issues() == null ? List.of() : page.issues();
        if (items.size() > page.maxResults()) {
            throw new IllegalStateException("Jira pagination returned more items than maxResults");
        }
    }

    public static class JiraPaginationLimitException extends IllegalStateException {
        public JiraPaginationLimitException(String message) {
            super(message);
        }
    }
}
