package vn.edu.cnpm.projectsupport.integration.jira.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
<<<<<<< HEAD
=======
import java.util.function.Consumer;
>>>>>>> 6f00c2c (CNPM-81 implement Jira project issue backlog sprint sync)
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;

public class JiraPaginationReader {

    public static final int DEFAULT_MAX_ITEMS = 1000;

    private final int pageSize;
    private final int maxItems;

    public JiraPaginationReader(int pageSize) {
        this(pageSize, DEFAULT_MAX_ITEMS);
    }

    public JiraPaginationReader(int pageSize, int maxItems) {

        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }

        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems must be greater than 0");
        }

        if (pageSize > maxItems) {
            throw new IllegalArgumentException("pageSize must not be greater than maxItems");
        }

        this.pageSize = pageSize;
        this.maxItems = maxItems;
    }

<<<<<<< HEAD
=======
    public <T> void readPages(
            BiFunction<Integer, Integer, JiraPageDto<T>> pageLoader,
            Consumer<List<T>> pageConsumer) {

        int startAt = 0;
        int processed = 0;

        while (processed < maxItems) {
            JiraPageDto<T> page = pageLoader.apply(startAt, pageSize);
            if (page == null) {
                break;
            }
            validatePage(page, startAt);
            List<T> items = page.issues() == null ? List.of() : page.issues();
            int remaining = maxItems - processed;
            if (items.size() > remaining) {
                pageConsumer.accept(items.subList(0, remaining));
                break;
            }
            pageConsumer.accept(items);
            processed += items.size();
            if (Boolean.TRUE.equals(page.isLast()) || processed >= page.total() || items.isEmpty()) {
                break;
            }
            int nextStartAt = page.startAt() + items.size();
            if (nextStartAt <= startAt) {
                throw new IllegalStateException("Jira pagination did not advance");
            }
            startAt = nextStartAt;
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

>>>>>>> 6f00c2c (CNPM-81 implement Jira project issue backlog sprint sync)
    public <T> List<T> readAll(BiFunction<Integer, Integer, JiraPageDto<T>> pageLoader) {

        List<T> result = new ArrayList<>();
        int startAt = 0;

        while (result.size() < maxItems) {
            JiraPageDto<T> page = pageLoader.apply(startAt, pageSize);

            if (page == null) {
                break;
            }

            if (page.startAt() != startAt) {
                throw new IllegalStateException("Jira pagination returned unexpected startAt");
            }

            if (page.maxResults() <= 0) {
                throw new IllegalStateException("Jira pagination returned invalid maxResults");
            }

            if (page.total() < 0 || page.total() < page.startAt()) {
                throw new IllegalStateException("Jira pagination returned invalid total");
            }

            List<T> items = page.issues() == null ? List.of(): page.issues();

            if (items.size() > page.maxResults()) {
                throw new IllegalStateException("Jira pagination returned more items than maxResults");
            }

            int remaining = maxItems - result.size();

            if (items.size() > remaining) {
                result.addAll(items.subList(0, remaining));
                break;
            }

            result.addAll(items);

            if (Boolean.TRUE.equals(page.isLast())) {
                break;
            }

            if (result.size() >= page.total()) {
                break;
            }

            if (items.isEmpty()) {
                break;
            }

            int nextStartAt = page.startAt() + items.size();

            if (nextStartAt <= startAt) {
                throw new IllegalStateException("Jira pagination did not advance");
            }
            startAt = nextStartAt;
        }
        return result;
    }
}